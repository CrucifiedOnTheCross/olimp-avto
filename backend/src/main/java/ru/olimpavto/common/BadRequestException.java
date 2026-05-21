package ru.olimpavto.common;

import java.util.Map;
import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final Map<String, String> fields;

    public BadRequestException(String message, Map<String, String> fields) {
        super(message);
        this.fields = fields;
    }
}
