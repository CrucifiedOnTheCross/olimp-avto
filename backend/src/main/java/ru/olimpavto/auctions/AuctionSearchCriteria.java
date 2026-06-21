package ru.olimpavto.auctions;

public record AuctionSearchCriteria(
        AuctionSource source,
        String query,
        String manufacturer,
        String model,
        Integer yearFrom,
        Integer yearTo,
        Integer minMileage,
        Integer maxMileage,
        Integer engineFrom,
        Integer engineTo,
        Long priceFrom,
        Long priceTo,
        String transmission,
        String drive,
        String sort,
        String lotNumber,
        Integer dayOfWeek,
        Integer limit,
        Integer offset
) {
}
