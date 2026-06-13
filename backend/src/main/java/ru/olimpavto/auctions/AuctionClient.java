package ru.olimpavto.auctions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<String, CacheEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();
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
        String cacheKey = "search:" + sql;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.isAlive(properties.getCacheTtl())) {
            return new AuctionSearchResponse()
                    .items(cached.items())
                    .source("avto.jp")
                    .cached(true);
        }

        List<AuctionLot> items = execute(sql);
        cache.put(cacheKey, new CacheEntry(items, Instant.now()));
        return new AuctionSearchResponse()
                .items(items)
                .source("avto.jp")
                .cached(false);
    }

    public AuctionLot getLot(String id) {
        List<AuctionLot> lots = execute(sqlBuilder.lotSql(id));
        if (lots.isEmpty()) {
            return null;
        }
        return lots.getFirst();
    }

    public List<String> manufacturers() {
        return executeRows("select distinct marka_name from main order by marka_name")
                .stream()
                .map(row -> first(row, "marka_name", "manufacturer", "brand", "make"))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> models(String manufacturer) {
        return executeRows("select distinct model_name from main where marka_name = '%s' order by model_name"
                .formatted(escape(manufacturer)))
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
                + "?json&code=" + encode(properties.getApiCode())
                + "&sql=" + encode(sql));

        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            return parseLots(body);
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
        if (cached != null && cached.isAlive(properties.getCacheTtl())) {
            return cached.rows();
        }

        URI uri = URI.create(properties.getApiUrl()
                + "?json&code=" + encode(properties.getApiCode())
                + "&sql=" + encode(sql));

        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            List<Map<String, String>> rows = parseRows(body);
            rawCache.put(cacheKey, new RawCacheEntry(rows, Instant.now()));
            return rows;
        } catch (RestClientException exception) {
            throw new AuctionApiException("API аукционов временно недоступен", exception);
        }
    }

    private List<AuctionLot> parseLots(String body) {
        return parseRows(body).stream().map(this::toLot).toList();
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
                .price(longValue(first(rawFields, "price", "start_price", "finish_price")))
                .auction(first(rawFields, "auction", "auction_name", "auctionName"))
                .lot(first(rawFields, "lot", "lot_no", "lotNo"))
                .grade(first(rawFields, "grade", "rate", "rating", "ocenka"))
                .color(first(rawFields, "color", "colour"))
                .engine(first(rawFields, "engine", "eng_v", "volume"))
                .imageUrl(imageUrl(rawFields))
                .auctionDate(first(rawFields, "auction_date", "date", "auctionDate"))
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
        String image = first(fields, "image", "image_url", "photo", "photo_url", "pictures");
        if (image == null || image.isBlank()) {
            return null;
        }
        if (image.startsWith("http://") || image.startsWith("https://")) {
            return image;
        }
        return "https://7.ajes.com/img/" + image;
    }

    private String first(Map<String, String> fields, String... names) {
        for (String name : names) {
            String value = fields.get(name);
            if (value != null && !value.isBlank()) {
                return value;
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

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "''");
    }

    private SimpleClientHttpRequestFactory requestFactory(AuctionProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeout());
        factory.setReadTimeout(properties.getTimeout());
        return factory;
    }

    private record CacheEntry(List<AuctionLot> items, Instant createdAt) {

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
