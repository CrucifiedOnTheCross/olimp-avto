package ru.olimpavto.auctions;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuctionSqlBuilder {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final String PRICE_EXPRESSION =
            "case when finish > 0 then finish when start > 0 then start else avg_price end";

    public String searchSql(AuctionSearchCriteria criteria) {
        List<String> where = new ArrayList<>();
        where.add("1=1");

        addSearch(where, criteria.query());
        addLike(where, "marka_name", criteria.manufacturer());
        addLike(where, "model_name", criteria.model());
        addLike(where, "lot", criteria.lotNumber());

        if (criteria.yearFrom() != null) {
            where.add("year >= " + criteria.yearFrom());
        }
        if (criteria.yearTo() != null) {
            where.add("year <= " + criteria.yearTo());
        }
        if (criteria.minMileage() != null) {
            where.add("mileage >= " + criteria.minMileage());
        }
        if (criteria.maxMileage() != null) {
            where.add("mileage <= " + criteria.maxMileage());
        }
        if (criteria.engineFrom() != null) {
            where.add("eng_v >= " + criteria.engineFrom());
        }
        if (criteria.engineTo() != null) {
            where.add("eng_v <= " + criteria.engineTo());
        }
        if (criteria.priceFrom() != null) {
            where.add(PRICE_EXPRESSION + " >= " + criteria.priceFrom());
        }
        if (criteria.priceTo() != null) {
            where.add(PRICE_EXPRESSION + " <= " + criteria.priceTo());
        }
        if (criteria.transmission() != null && !criteria.transmission().isBlank()) {
            where.add("kpp like '%" + escape(criteria.transmission().trim()) + "%'");
        }
        if ("4wd".equalsIgnoreCase(criteria.drive())) {
            where.add("priv like '%4WD%'");
        } else if ("2wd".equalsIgnoreCase(criteria.drive())) {
            where.add("(priv is null or priv not like '%4WD%')");
        }
        if (criteria.dayOfWeek() != null) {
            int apiDay = criteria.dayOfWeek() == 7 ? 1 : criteria.dayOfWeek() + 1;
            where.add("dayofweek(auction_date) = " + apiDay);
        }

        return "select * from %s where %s order by %s limit %d,%d"
                .formatted(
                        criteria.source().table(),
                        String.join(" and ", where),
                        orderBy(criteria.sort()),
                        offset(criteria.offset()),
                        limit(criteria.limit())
                );
    }

    public String lotSql(AuctionSource source, String id) {
        return "select * from %s where id = '%s' limit 1".formatted(source.table(), escape(id));
    }

    public String manufacturersSql(AuctionSource source) {
        return "select distinct marka_name from %s order by marka_name".formatted(source.table());
    }

    public String modelsSql(AuctionSource source, String manufacturer) {
        return "select distinct model_name from %s where marka_name = '%s' order by model_name"
                .formatted(source.table(), escape(manufacturer));
    }

    private void addSearch(List<String> where, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String escaped = escape(value.trim());
        where.add("(marka_name like '%%%1$s%%' or model_name like '%%%1$s%%' or lot like '%%%1$s%%')"
                .formatted(escaped));
    }

    private void addLike(List<String> where, String field, String value) {
        if (value != null && !value.isBlank()) {
            where.add("%s like '%%%s%%'".formatted(field, escape(value.trim())));
        }
    }

    private int limit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(requestedLimit, MAX_LIMIT));
    }

    private int offset(Integer requestedOffset) {
        return requestedOffset == null ? 0 : Math.max(0, Math.min(requestedOffset, 1000));
    }

    private String orderBy(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "yearDesc" -> "year desc,id desc";
            case "yearAsc" -> "year asc,id asc";
            case "priceAsc" -> PRICE_EXPRESSION + " asc,id desc";
            case "priceDesc" -> PRICE_EXPRESSION + " desc,id desc";
            case "mileageAsc" -> "mileage asc,id desc";
            case "mileageDesc" -> "mileage desc,id desc";
            default -> "year desc,auction_date desc,id desc";
        };
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "''");
    }
}
