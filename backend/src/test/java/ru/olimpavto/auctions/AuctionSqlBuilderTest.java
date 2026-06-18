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
                80_000,
                "125",
                1,
                500
        ));

        assertThat(sql).contains("model_name like '%Prius'' or ''1''=''1%'");
        assertThat(sql).contains("marka_name like '%Toyota%'");
        assertThat(sql).contains("lot like '%125%'");
        assertThat(sql).contains("dayofweek(auction_date) = 2");
        assertThat(sql).contains("year >= 2020");
        assertThat(sql).contains("year <= 2024");
        assertThat(sql).contains("mileage <= 80000");
        assertThat(sql).endsWith("limit 50");
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
