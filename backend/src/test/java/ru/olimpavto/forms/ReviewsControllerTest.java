package ru.olimpavto.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.mail.MailService;

class ReviewsControllerTest {

    private final RecordingMailService mailService = new RecordingMailService();
    private final ReviewsController controller = new ReviewsController(mailService);

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
        assertThat(response.getBody().getMessage()).isEqualTo("Отзыв отправлен");
        assertThat(mailService.rating).isEqualTo(5);
        assertThat(mailService.text).isEqualTo("Отличная работа команды");
        assertThat(mailService.name).isEqualTo("Иван");
        assertThat(mailService.carModel).isEqualTo("Toyota Camry");
        assertThat(mailService.country).isEqualTo("Япония");
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

        assertThat(mailService.text).isNull();
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

    private MockMultipartFile image(String filename) {
        return new MockMultipartFile("photos", filename, "image/jpeg", new byte[] {1, 2, 3});
    }

    private MockMultipartFile textFile(String filename) {
        return new MockMultipartFile("photos", filename, "text/plain", "text".getBytes());
    }

    private static class RecordingMailService extends MailService {

        private Integer rating;
        private String text;
        private String name;
        private String carModel;
        private String country;
        private List<MultipartFile> photos;

        RecordingMailService() {
            super(null, null);
        }

        @Override
        public void sendReview(
                Integer rating,
                String text,
                String name,
                String carModel,
                String country,
                List<MultipartFile> photos
        ) {
            this.rating = rating;
            this.text = text;
            this.name = name;
            this.carModel = carModel;
            this.country = country;
            this.photos = photos;
        }
    }
}
