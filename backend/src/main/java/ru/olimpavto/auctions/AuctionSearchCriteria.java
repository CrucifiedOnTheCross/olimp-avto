package ru.olimpavto.auctions;

public record AuctionSearchCriteria(
        String query,
        String manufacturer,
        String model,
        Integer yearFrom,
        Integer yearTo,
        Integer maxMileage,
        Integer limit
) {
}
