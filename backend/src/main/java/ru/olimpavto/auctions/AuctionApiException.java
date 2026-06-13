package ru.olimpavto.auctions;

public class AuctionApiException extends RuntimeException {

    public AuctionApiException(String message) {
        super(message);
    }

    public AuctionApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
