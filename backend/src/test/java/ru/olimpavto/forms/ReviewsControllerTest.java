package ru.olimpavto.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.mail.MailService;
import ru.olimpavto.config.AppProperties;
import ru.olimpavto.reviews.ReviewEntity;
import ru.olimpavto.reviews.ReviewMapper;
import ru.olimpavto.reviews.ReviewRepository;
import ru.olimpavto.reviews.ReviewStatus;

class ReviewsControllerTest {

    private RecordingMailService mailService;
    private FakeReviewRepository reviewRepository;
    private ReviewsController controller;

    @BeforeEach
    void setUp() {
        mailService = new RecordingMailService();
        reviewRepository = new FakeReviewRepository();
        AppProperties appProperties = new AppProperties();
        appProperties.setPublicBaseUrl("http://localhost:8080");
        controller = new ReviewsController(
                mailService,
                reviewRepository.proxy(),
                new ReviewMapper(),
                appProperties
        );
    }

    @Test
    void sendReviewSendsMailAndReturnsAccepted() {
        List<MultipartFile> photos = List.of(image("photo.jpg"));

        ResponseEntity<FormResponse> response = controller.sendReview(
                5,
                "Отличная работа команды",
                " Иван ",
                " Toyota Camry ",
                "Япония",
                photos
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Отзыв отправлен на модерацию");
        assertThat(reviewRepository.saved).hasSize(1);
        assertThat(reviewRepository.saved.getFirst().getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(reviewRepository.saved.getFirst().getText()).isEqualTo("Отличная работа команды");
        assertThat(mailService.review).isSameAs(reviewRepository.saved.getFirst());
        assertThat(mailService.approveUrl).contains("/api/reviews/1/approve?token=");
        assertThat(mailService.rejectUrl).contains("/api/reviews/1/reject?token=");
        assertThat(mailService.photos).isSameAs(photos);
    }

    @Test
    void sendReviewRejectsInvalidFieldsAndDoesNotSendMail() {
        assertThatThrownBy(() -> controller.sendReview(
                6,
                "short",
                "A",
                "",
                "Германия",
                List.of(textFile("notes.txt"))
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting("fields")
                .satisfies(fields -> assertThat(fields)
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsKeys("rating", "text", "name", "carModel", "country", "photos"));

        assertThat(mailService.review).isNull();
    }

    @Test
    void sendReviewRejectsMoreThanFivePhotos() {
        List<MultipartFile> photos = List.of(
                image("1.jpg"),
                image("2.jpg"),
                image("3.jpg"),
                image("4.jpg"),
                image("5.jpg"),
                image("6.jpg")
        );

        assertThatThrownBy(() -> controller.sendReview(
                5,
                "Отзыв достаточно длинный",
                "Иван",
                "Toyota Camry",
                "Япония",
                photos
        ))
                .isInstanceOf(BadRequestException.class)
                .extracting("fields")
                .satisfies(fields -> assertThat(fields)
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("photos", "Можно загрузить максимум 5 фотографий"));
    }

    @Test
    void approveReviewMakesReviewVisibleOnPublicList() {
        ReviewEntity review = pendingReview();
        reviewRepository.rows.add(review);

        ResponseEntity<String> response = controller.approveReview(review.getId(), review.getModerationToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(review.getModeratedAt()).isNotNull();
        assertThat(controller.listApprovedReviews().getBody()).hasSize(1);
    }

    @Test
    void rejectReviewKeepsReviewOutOfPublicList() {
        ReviewEntity review = pendingReview();
        reviewRepository.rows.add(review);

        ResponseEntity<String> response = controller.rejectReview(review.getId(), review.getModerationToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(controller.listApprovedReviews().getBody()).isEmpty();
    }

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("photos", filename, "image/jpeg", new byte[] {1, 2, 3});
    }

    private MockMultipartFile textFile(String filename) {
        return new MockMultipartFile("photos", filename, "text/plain", "text".getBytes());
    }

    private ReviewEntity pendingReview() {
        ReviewEntity review = new ReviewEntity();
        review.setId(1L);
        review.setRating(5);
        review.setText("Отзыв достаточно длинный");
        review.setName("Иван");
        review.setCarModel("Toyota Camry");
        review.setCountry("Япония");
        review.setStatus(ReviewStatus.PENDING);
        review.setModerationToken("token-12345678901234567890");
        review.setCreatedAt(OffsetDateTime.now());
        return review;
    }

    private static class RecordingMailService extends MailService {

        private ReviewEntity review;
        private String approveUrl;
        private String rejectUrl;
        private List<MultipartFile> photos;

        RecordingMailService() {
            super(null, null);
        }

        @Override
        public void sendReviewModeration(
                ReviewEntity review,
                String approveUrl,
                String rejectUrl,
                List<MultipartFile> photos
        ) {
            this.review = review;
            this.approveUrl = approveUrl;
            this.rejectUrl = rejectUrl;
            this.photos = photos;
        }
    }

    private static class FakeReviewRepository {

        private final List<ReviewEntity> rows = new ArrayList<>();
        private final List<ReviewEntity> saved = new ArrayList<>();
        private long nextId = 1L;

        ReviewRepository proxy() {
            return (ReviewRepository) Proxy.newProxyInstance(
                    ReviewRepository.class.getClassLoader(),
                    new Class<?>[] {ReviewRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((ReviewEntity) args[0]);
                        case "findByStatusOrderByCreatedAtDesc" -> findByStatus((ReviewStatus) args[0]);
                        case "findByIdAndModerationToken" -> findByIdAndModerationToken((Long) args[0], (String) args[1]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private ReviewEntity save(ReviewEntity review) {
            if (review.getId() == null) {
                review.setId(nextId++);
            }
            if (!rows.contains(review)) {
                rows.add(review);
            }
            saved.add(review);
            return review;
        }

        private List<ReviewEntity> findByStatus(ReviewStatus status) {
            return rows.stream()
                    .filter(review -> status == review.getStatus())
                    .toList();
        }

        private Optional<ReviewEntity> findByIdAndModerationToken(Long id, String token) {
            return rows.stream()
                    .filter(review -> id.equals(review.getId()))
                    .filter(review -> token.equals(review.getModerationToken()))
                    .findFirst();
        }
    }
}
