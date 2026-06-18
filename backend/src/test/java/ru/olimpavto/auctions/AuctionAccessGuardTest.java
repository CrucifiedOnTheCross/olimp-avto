package ru.olimpavto.auctions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuctionAccessGuardTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requiresCaptchaAfterThresholdAndAllowsClientAfterVerification() {
        AuctionProperties properties = new AuctionProperties();
        properties.setCaptchaThreshold(1);
        Clock clock = Clock.fixed(Instant.parse("2026-06-18T10:00:00Z"), ZoneOffset.UTC);
        AuctionAccessGuard guard = new AuctionAccessGuard(properties, clock);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatCode(guard::checkLotView).doesNotThrowAnyException();

        AuctionCaptchaRequiredException challenge = org.assertj.core.api.Assertions.catchThrowableOfType(
                guard::checkLotView,
                AuctionCaptchaRequiredException.class
        );
        String question = challenge.getFields().get("captchaQuestion");
        String[] numbers = question.replace("= ?", "").trim().split(" \\+ ");
        int answer = Integer.parseInt(numbers[0]) + Integer.parseInt(numbers[1]);

        guard.verify(challenge.getFields().get("captchaId"), answer);
        assertThatCode(guard::checkLotView).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.verify(challenge.getFields().get("captchaId"), answer))
                .isInstanceOf(ru.olimpavto.common.BadRequestException.class);
    }
}
