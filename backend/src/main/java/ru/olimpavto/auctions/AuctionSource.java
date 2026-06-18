package ru.olimpavto.auctions;

import java.util.Locale;

public enum AuctionSource {
    JAPAN("japan", "main"),
    KOREA("korea", "korea"),
    CHINA("china", "china");

    private final String apiValue;
    private final String table;

    AuctionSource(String apiValue, String table) {
        this.apiValue = apiValue;
        this.table = table;
    }

    public String apiValue() {
        return apiValue;
    }

    public String table() {
        return table;
    }

    public static AuctionSource fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            return JAPAN;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AuctionSource source : values()) {
            if (source.apiValue.equals(normalized)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Неизвестный источник аукционов");
    }
}
