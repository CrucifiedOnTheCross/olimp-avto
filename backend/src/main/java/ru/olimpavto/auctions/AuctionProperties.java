package ru.olimpavto.auctions;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auctions")
public class AuctionProperties {

    private String apiUrl = "http://78.46.90.228/gzip/";
    private String apiCode = "";
    private Duration timeout = Duration.ofSeconds(5);
    private Duration cacheTtl = Duration.ofMinutes(10);
}
