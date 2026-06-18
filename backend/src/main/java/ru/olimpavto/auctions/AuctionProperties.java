package ru.olimpavto.auctions;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auctions")
public class AuctionProperties {

    private String apiUrl = "http://87.242.72.57/gzip/";
    private String apiIp = "8.1.1.1";
    private String apiCode = "";
    private Duration timeout = Duration.ofSeconds(20);
    private Duration dictionaryCacheTtl = Duration.ofMinutes(30);
    private Duration lotCacheTtl = Duration.ofMinutes(10);
    private int captchaThreshold = 50;
    private Duration captchaTtl = Duration.ofMinutes(5);
}
