package ru.olimpavto.auctions;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AuctionSqlBuilder {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

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
        if (criteria.maxMileage() != null) {
            where.add("mileage <= " + criteria.maxMileage());
        }
        if (criteria.dayOfWeek() != null) {
            int apiDay = criteria.dayOfWeek() == 7 ? 1 : criteria.dayOfWeek() + 1;
            where.add("dayofweek(auction_date) = " + apiDay);
        }

        return "select * from %s where %s order by auction_date desc limit %d"
                .formatted(criteria.source().table(), String.join(" and ", where), limit(criteria.limit()));
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

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "''");
    }
}
