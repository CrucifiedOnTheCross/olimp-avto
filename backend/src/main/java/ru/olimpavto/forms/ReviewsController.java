package ru.olimpavto.forms;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.olimpavto.api.ReviewsApiDelegate;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.mail.MailService;

@Service
public class ReviewsController implements ReviewsApiDelegate {

    private static final List<String> ALLOWED_COUNTRIES = List.of("Корея", "Япония", "Китай", "США");

    private final MailService mailService;

    public ReviewsController(MailService mailService) {
        this.mailService = mailService;
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
        mailService.sendReview(rating, text.trim(), name.trim(), carModel.trim(), country, photos);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new FormResponse("Отзыв отправлен"));
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
}
