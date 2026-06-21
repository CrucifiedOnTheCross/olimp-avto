package ru.olimpavto.auctions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuctionSqlBuilderTest {

    private final AuctionSqlBuilder builder = new AuctionSqlBuilder();

    @Test
    void searchSqlEscapesUserTextAndLimitsPageSize() {
        String sql = builder.searchSql(new AuctionSearchCriteria(
                AuctionSource.JAPAN,
                "Prius' or '1'='1",
                "Toyota",
                null,
                2020,
                2024,
                5_000,
                80_000,
                1_000,
                2_500,
                100_000L,
                2_000_000L,
                "AT",
                "4wd",
                "priceAsc",
                "125",
                1,
                500,
                24
        ));

        assertThat(sql).contains("model_name like '%Prius'' or ''1''=''1%'");
        assertThat(sql).contains("marka_name like '%Toyota%'");
        assertThat(sql).contains("lot like '%125%'");
        assertThat(sql).contains("dayofweek(auction_date) = 2");
        assertThat(sql).contains("year >= 2020");
        assertThat(sql).contains("year <= 2024");
        assertThat(sql).contains("mileage >= 5000");
        assertThat(sql).contains("mileage <= 80000");
        assertThat(sql).contains("eng_v >= 1000");
        assertThat(sql).contains("eng_v <= 2500");
        assertThat(sql).contains("kpp like '%AT%'");
        assertThat(sql).contains("priv like '%4WD%'");
        assertThat(sql).contains("case when finish > 0 then finish when start > 0 then start else avg_price end >= 100000");
        assertThat(sql).contains("order by case when finish > 0 then finish when start > 0 then start else avg_price end asc,id desc");
        assertThat(sql).endsWith("limit 24,50");
    }

    @Test
    void unknownSortFallsBackToSafeWhitelistedOrder() {
        String sql = builder.searchSql(new AuctionSearchCriteria(
                AuctionSource.CHINA,
                null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, "drop table main", null, null, 20, 0
        ));

        assertThat(sql).contains("order by year desc,auction_date desc,id desc");
        assertThat(sql).doesNotContain("drop table");
    }

    @Test
    void lotSqlEscapesExternalId() {
        assertThat(builder.lotSql(AuctionSource.KOREA, "abc'123"))
                .isEqualTo("select * from korea where id = 'abc''123' limit 1");
    }

    @Test
    void sourceControlsOnlyWhitelistedTableName() {
        assertThat(builder.lotSql(AuctionSource.CHINA, "123"))
                .isEqualTo("select * from china where id = '123' limit 1");
    }
}
