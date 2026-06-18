package ru.olimpavto.auctions;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.olimpavto.common.BadRequestException;

@Service
public class AuctionAccessGuard {

    private final AuctionProperties properties;
    private final Clock clock;
    private final Map<String, AtomicInteger> dailyViews = new ConcurrentHashMap<>();
    private final Map<String, LocalDate> verifiedClients = new ConcurrentHashMap<>();
    private final Map<String, CaptchaChallenge> challenges = new ConcurrentHashMap<>();

    @Autowired
    public AuctionAccessGuard(AuctionProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    AuctionAccessGuard(AuctionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void checkLotView() {
        cleanup();
        String client = clientAddress();
        LocalDate today = LocalDate.now(clock);
        if (today.equals(verifiedClients.get(client))) {
            return;
        }

        int views = dailyViews.computeIfAbsent(client + '|' + today, ignored -> new AtomicInteger())
                .incrementAndGet();
        if (views <= properties.getCaptchaThreshold()) {
            return;
        }

        int left = ThreadLocalRandom.current().nextInt(2, 10);
        int right = ThreadLocalRandom.current().nextInt(2, 10);
        String id = UUID.randomUUID().toString();
        challenges.put(id, new CaptchaChallenge(
                client,
                left + right,
                Instant.now(clock).plus(properties.getCaptchaTtl())
        ));
        throw new AuctionCaptchaRequiredException(id, left + " + " + right + " = ?");
    }

    public void verify(String captchaId, int answer) {
        cleanup();
        CaptchaChallenge challenge = challenges.remove(captchaId);
        if (challenge == null
                || challenge.expiresAt().isBefore(Instant.now(clock))
                || !challenge.client().equals(clientAddress())
                || challenge.answer() != answer) {
            throw new BadRequestException(
                    "Неверный или устаревший ответ",
                    Map.of("answer", "Обновите карточку лота и попробуйте снова")
            );
        }
        verifiedClients.put(challenge.client(), LocalDate.now(clock));
    }

    private String clientAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }

    private void cleanup() {
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock);
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        verifiedClients.entrySet().removeIf(entry -> !entry.getValue().equals(today));
        dailyViews.keySet().removeIf(key -> !key.endsWith("|" + today));
    }

    private record CaptchaChallenge(String client, int answer, Instant expiresAt) {
    }
}
