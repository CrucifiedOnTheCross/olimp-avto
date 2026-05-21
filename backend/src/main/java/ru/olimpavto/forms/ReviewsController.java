package ru.olimpavto.forms;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;
import ru.olimpavto.api.ReviewsApiDelegate;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.common.NotFoundException;
import ru.olimpavto.config.AppProperties;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.dto.ReviewResponse;
import ru.olimpavto.mail.MailService;
import ru.olimpavto.reviews.ReviewEntity;
import ru.olimpavto.reviews.ReviewMapper;
import ru.olimpavto.reviews.ReviewRepository;
import ru.olimpavto.reviews.ReviewStatus;

@Service
@EnableConfigurationProperties(AppProperties.class)
public class ReviewsController implements ReviewsApiDelegate {

    private static final List<String> ALLOWED_COUNTRIES = List.of("Корея", "Япония", "Китай", "США");

    private final MailService mailService;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final AppProperties appProperties;

    public ReviewsController(
            MailService mailService,
            ReviewRepository reviewRepository,
            ReviewMapper reviewMapper,
            AppProperties appProperties
    ) {
        this.mailService = mailService;
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
        this.appProperties = appProperties;
    }

    @Override
    public ResponseEntity<FormResponse> sendReview(
            Integer rating,
            String text,
            String name,
            String carModel,
            String country,
            List<MultipartFile> photos
    ) {
        validateReview(rating, text, name, carModel, country, photos);
        ReviewEntity review = new ReviewEntity();
        review.setRating(rating);
        review.setText(text.trim());
        review.setName(name.trim());
        review.setCarModel(carModel.trim());
        review.setCountry(country);
        review.setStatus(ReviewStatus.PENDING);
        review.setModerationToken(generateModerationToken());
        review.setCreatedAt(OffsetDateTime.now());

        ReviewEntity saved = reviewRepository.save(review);

        mailService.sendReviewModeration(
                saved,
                moderationUrl(saved, "approve"),
                moderationUrl(saved, "reject"),
                photos
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new FormResponse("Отзыв отправлен на модерацию"));
    }

    @Override
    public ResponseEntity<List<ReviewResponse>> listApprovedReviews() {
        return ResponseEntity.ok(reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED).stream()
                .map(reviewMapper::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<String> approveReview(Long id, String token) {
        ReviewEntity review = findByModerationToken(id, token);
        review.setStatus(ReviewStatus.APPROVED);
        review.setModeratedAt(OffsetDateTime.now());
        reviewRepository.save(review);
        return html("Отзыв принят", "Отзыв теперь будет отображаться на сайте.");
    }

    @Override
    public ResponseEntity<String> rejectReview(Long id, String token) {
        ReviewEntity review = findByModerationToken(id, token);
        review.setStatus(ReviewStatus.REJECTED);
        review.setModeratedAt(OffsetDateTime.now());
        reviewRepository.save(review);
        return html("Отзыв отклонён", "Отзыв не будет отображаться на сайте.");
    }

    private void validateReview(
            Integer rating,
            String text,
            String name,
            String carModel,
            String country,
            List<MultipartFile> photos
    ) {
        Map<String, String> fields = new LinkedHashMap<>();

        if (rating == null || rating < 1 || rating > 5) {
            fields.put("rating", "Оценка должна быть от 1 до 5");
        }
        validateText(fields, "text", text, 10, 2000, "Текст отзыва");
        validateText(fields, "name", name, 2, 120, "Имя");
        validateText(fields, "carModel", carModel, 2, 120, "Модель авто");

        if (country == null || !ALLOWED_COUNTRIES.contains(country)) {
            fields.put("country", "Выберите страну из списка");
        }

        if (photos != null) {
            if (photos.size() > 5) {
                fields.put("photos", "Можно загрузить максимум 5 фотографий");
            }

            for (MultipartFile photo : photos) {
                String contentType = photo.getContentType();
                if (!photo.isEmpty() && (contentType == null || !contentType.startsWith("image/"))) {
                    fields.put("photos", "Можно прикладывать только изображения");
                    break;
                }
            }
        }

        if (!fields.isEmpty()) {
            throw new BadRequestException("Проверьте поля формы", fields);
        }
    }

    private void validateText(
            Map<String, String> fields,
            String field,
            String value,
            int min,
            int max,
            String label
    ) {
        if (value == null || value.trim().length() < min) {
            fields.put(field, "%s должно содержать минимум %d символа".formatted(label, min));
            return;
        }
        if (value.trim().length() > max) {
            fields.put(field, "%s слишком длинное".formatted(label));
        }
    }

    private ReviewEntity findByModerationToken(Long id, String token) {
        return reviewRepository.findByIdAndModerationToken(id, token)
                .orElseThrow(() -> new NotFoundException("Отзыв не найден или ссылка модерации устарела"));
    }

    private String moderationUrl(ReviewEntity review, String action) {
        return UriComponentsBuilder.fromUriString(appProperties.getPublicBaseUrl())
                .path("/api/reviews/{id}/{action}")
                .queryParam("token", review.getModerationToken())
                .buildAndExpand(review.getId(), action)
                .toUriString();
    }

    private String generateModerationToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private ResponseEntity<String> html(String title, String message) {
        String body = """
                <!doctype html>
                <html lang="ru">
                <head>
                    <meta charset="utf-8">
                    <title>%s</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 32px;">
                    <h1>%s</h1>
                    <p>%s</p>
                </body>
                </html>
                """.formatted(title, title, message);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(body);
    }
}
