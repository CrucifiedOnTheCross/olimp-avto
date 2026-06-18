package ru.olimpavto.auctions;

import java.util.Map;
import lombok.Getter;

@Getter
public class AuctionCaptchaRequiredException extends RuntimeException {

    private final Map<String, String> fields;

    public AuctionCaptchaRequiredException(String captchaId, String question) {
        super("Подтвердите, что вы не робот");
        this.fields = Map.of(
                "captchaId", captchaId,
                "captchaQuestion", question
        );
    }
}
