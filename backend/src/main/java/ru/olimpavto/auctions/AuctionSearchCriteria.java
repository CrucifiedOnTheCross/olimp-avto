package ru.olimpavto.auctions;

public record AuctionSearchCriteria(
        AuctionSource source,
        String query,
        String manufacturer,
        String model,
        Integer yearFrom,
        Integer yearTo,
        Integer maxMileage,
        String lotNumber,
        Integer dayOfWeek,
        Integer limit,
        Integer offset
) {
}
