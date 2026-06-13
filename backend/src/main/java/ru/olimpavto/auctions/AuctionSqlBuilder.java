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

        addLike(where, "model_name", criteria.query());
        addLike(where, "marka_name", criteria.manufacturer());
        addLike(where, "model_name", criteria.model());

        if (criteria.yearFrom() != null) {
            where.add("year >= " + criteria.yearFrom());
        }
        if (criteria.yearTo() != null) {
            where.add("year <= " + criteria.yearTo());
        }
        if (criteria.maxMileage() != null) {
            where.add("mileage <= " + criteria.maxMileage());
        }

        return "select * from main where %s order by auction_date desc limit %d"
                .formatted(String.join(" and ", where), limit(criteria.limit()));
    }

    public String lotSql(String id) {
        return "select * from main where id = '%s' limit 1".formatted(escape(id));
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
