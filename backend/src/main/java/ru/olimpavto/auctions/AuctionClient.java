package ru.olimpavto.auctions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.olimpavto.dto.AuctionLot;
import ru.olimpavto.dto.AuctionSearchResponse;

@Service
@EnableConfigurationProperties(AuctionProperties.class)
public class AuctionClient {

    private final AuctionProperties properties;
    private final AuctionSqlBuilder sqlBuilder;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Map<String, LotCacheEntry> lotCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, RawCacheEntry> rawCache = new java.util.concurrent.ConcurrentHashMap<>();

    public AuctionClient(
            AuctionProperties properties,
            AuctionSqlBuilder sqlBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.sqlBuilder = sqlBuilder;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory(properties))
                .build();
    }

    public AuctionSearchResponse search(AuctionSearchCriteria criteria) {
        String sql = sqlBuilder.searchSql(criteria);
        List<AuctionLot> items = execute(sql);
        return new AuctionSearchResponse()
                .items(items)
                .source(criteria.source().apiValue())
                .cached(false);
    }

    public AuctionLot getLot(AuctionSource source, String id) {
        String cacheKey = source.apiValue() + ':' + id;
        LotCacheEntry cached = lotCache.get(cacheKey);
        if (cached != null && cached.isAlive(properties.getLotCacheTtl())) {
            return cached.lot();
        }

        List<AuctionLot> lots = execute(sqlBuilder.lotSql(source, id));
        if (lots.isEmpty()) {
            return null;
        }
        AuctionLot lot = lots.getFirst();
        lotCache.put(cacheKey, new LotCacheEntry(lot, Instant.now()));
        return lot;
    }

    public List<String> manufacturers(AuctionSource source) {
        return executeRows(sqlBuilder.manufacturersSql(source))
                .stream()
                .map(row -> first(row, "marka_name", "manufacturer", "brand", "make"))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> models(AuctionSource source, String manufacturer) {
        return executeRows(sqlBuilder.modelsSql(source, manufacturer))
                .stream()
                .map(row -> first(row, "model_name", "model", "name"))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private List<AuctionLot> execute(String sql) {
        if (properties.getApiCode() == null || properties.getApiCode().isBlank()) {
            throw new AuctionApiException("API-код аукционов не настроен");
        }

        URI uri = URI.create(properties.getApiUrl()
                + "?json&ip=" + encode(properties.getApiIp())
                + "&code=" + encode(properties.getApiCode())
                + "&sql=" + encode(sql));

        try {
            byte[] body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);
            return parseLots(decodeBody(body));
        } catch (RestClientException exception) {
            throw new AuctionApiException("API аукционов временно недоступен", exception);
        }
    }

    private List<Map<String, String>> executeRows(String sql) {
        if (properties.getApiCode() == null || properties.getApiCode().isBlank()) {
            throw new AuctionApiException("API-код аукционов не настроен");
        }

        String cacheKey = "rows:" + sql;
        RawCacheEntry cached = rawCache.get(cacheKey);
        if (cached != null && cached.isAlive(properties.getDictionaryCacheTtl())) {
            return cached.rows();
        }

        URI uri = URI.create(properties.getApiUrl()
                + "?json&ip=" + encode(properties.getApiIp())
                + "&code=" + encode(properties.getApiCode())
                + "&sql=" + encode(sql));

        try {
            byte[] body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(byte[].class);
            List<Map<String, String>> rows = parseRows(decodeBody(body));
            rawCache.put(cacheKey, new RawCacheEntry(rows, Instant.now()));
            return rows;
        } catch (RestClientException exception) {
            throw new AuctionApiException("API аукционов временно недоступен", exception);
        }
    }

    private List<AuctionLot> parseLots(String body) {
        return parseRows(body).stream().map(this::toLot).toList();
    }

    private String decodeBody(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        if (body.length >= 2 && (body[0] & 0xff) == 0x1f && (body[1] & 0xff) == 0x8b) {
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new AuctionApiException("API аукционов вернул поврежденный gzip-ответ", exception);
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private List<Map<String, String>> parseRows(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode rows = rows(root);
            List<Map<String, String>> result = new ArrayList<>();
            if (rows.isArray()) {
                rows.forEach(row -> result.add(rawFields(row)));
            }
            return result;
        } catch (Exception exception) {
            throw new AuctionApiException("API аукционов вернул неожиданный формат ответа", exception);
        }
    }

    private JsonNode rows(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        for (String field : List.of("data", "rows", "items", "result")) {
            JsonNode node = root.get(field);
            if (node != null && node.isArray()) {
                return node;
            }
        }
        return objectMapper.createArrayNode();
    }

    private AuctionLot toLot(Map<String, String> rawFields) {
        String id = first(rawFields, "id", "ID", "lot_id", "lotId", "auction_id");
        if (id == null || id.isBlank()) {
            id = first(rawFields, "lot", "LOT", "lot_no", "lotNo");
        }

        return new AuctionLot()
                .id(valueOrDash(id))
                .manufacturer(first(rawFields, "marka_name", "make", "manufacturer", "brand"))
                .model(first(rawFields, "model_name", "model", "name"))
                .year(integer(first(rawFields, "year", "YEAR")))
                .mileage(integer(first(rawFields, "mileage", "probeg", "miles")))
                .price(auctionPrice(rawFields))
                .auction(first(rawFields, "auction", "auction_name", "auctionName"))
                .lot(first(rawFields, "lot", "lot_no", "lotNo"))
                .grade(first(rawFields, "grade", "rate", "rating", "ocenka"))
                .color(first(rawFields, "color", "colour"))
                .engine(first(rawFields, "engine", "eng_v", "volume"))
                .imageUrl(imageUrl(rawFields))
                .imageUrls(imageUrls(rawFields))
                .auctionDate(auctionDate(rawFields))
                .rawFields(rawFields);
    }

    private Map<String, String> rawFields(JsonNode row) {
        Map<String, String> fields = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = row.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            JsonNode value = entry.getValue();
            fields.put(entry.getKey(), value.isNull() ? "" : value.asText());
        }
        return fields;
    }

    private String imageUrl(Map<String, String> fields) {
        List<String> images = imageUrls(fields);
        return images.isEmpty() ? null : mediumImage(images.getFirst());
    }

    private List<String> imageUrls(Map<String, String> fields) {
        String value = first(fields,
                "images", "pictures", "photos", "image", "image_url", "photo", "photo_url", "img");
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("[#,;|\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(this::normalizeImage)
                .distinct()
                .limit(20)
                .toList();
    }

    private String normalizeImage(String image) {
        image = image.replaceAll("&(?:h|w)=\\d+$", "");
        if (image.startsWith("//")) {
            return "https:" + image;
        }
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return image.replaceFirst("^http://", "https://");
        }
        String token = image.replaceFirst("^/+", "");
        if (token.startsWith("imgs/")) {
            token = token.substring(5);
        }
        return "https://7.tru.ru/imgs/" + token;
    }

    private Long auctionPrice(Map<String, String> fields) {
        for (String field : List.of("finish", "finish_price", "price", "start", "start_price", "avg_price")) {
            Long value = longValue(first(fields, field));
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private String auctionDate(Map<String, String> fields) {
        String value = first(fields, "auction_date", "date", "auctionDate");
        return value == null || value.startsWith("0000-00-00") ? null : value;
    }

    private String mediumImage(String image) {
        if (image.contains("7.tru.ru/imgs/") && !image.contains("&w=")) {
            return image + "&w=320";
        }
        return image;
    }

    private String first(Map<String, String> fields, String... names) {
        for (String name : names) {
            String value = fields.get(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)
                        && entry.getValue() != null
                        && !entry.getValue().isBlank()) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Integer integer(String value) {
        Long longValue = longValue(value);
        return longValue == null ? null : longValue.intValue();
    }

    private Long longValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        return Long.parseLong(digits);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private SimpleClientHttpRequestFactory requestFactory(AuctionProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeout());
        factory.setReadTimeout(properties.getTimeout());
        return factory;
    }

    private record LotCacheEntry(AuctionLot lot, Instant createdAt) {

        private boolean isAlive(java.time.Duration ttl) {
            return createdAt.plus(ttl).isAfter(Instant.now());
        }
    }

    private record RawCacheEntry(List<Map<String, String>> rows, Instant createdAt) {

        private boolean isAlive(java.time.Duration ttl) {
            return createdAt.plus(ttl).isAfter(Instant.now());
        }
    }

}
