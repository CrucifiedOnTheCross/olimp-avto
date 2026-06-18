package ru.olimpavto.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.olimpavto.dto.ConsultationRequest;
import ru.olimpavto.reviews.ReviewEntity;

@Service
@EnableConfigurationProperties(MailProperties.class)
public class MailService {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    public MailService(JavaMailSender mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendLead(ConsultationRequest request) {
        String body = """
                Новая заявка с сайта Олимп Авто

                Имя: %s
                Телефон: %s
                Комментарий: %s
                Согласие на обработку данных: %s
                """.formatted(
                request.getName(),
                request.getPhone(),
                valueOrDash(request.getComment()),
                Boolean.TRUE.equals(request.getPolicyAccepted()) ? "да" : "нет"
        );

        sendMessage("Новая заявка с сайта", body, List.of(), leadRecipients());
    }

    public void sendReview(
            Integer rating,
            String text,
            String name,
            String carModel,
            String country,
            List<MultipartFile> photos
    ) {
        String body = """
                Новый отзыв с сайта Олимп Авто

                Оценка: %d
                Имя: %s
                Автомобиль: %s
                Страна: %s

                Текст отзыва:
                %s
                """.formatted(rating, name, carModel, country, text);

        sendMessage("Новый отзыв с сайта", body, photos == null ? List.of() : photos, reviewRecipients());
    }

    public void sendReviewModeration(
            ReviewEntity review,
            String approveUrl,
            String rejectUrl,
            List<MultipartFile> photos
    ) {
        String body = """
                Новый отзыв ожидает модерации

                Оценка: %d
                Имя: %s
                Автомобиль: %s
                Страна: %s

                Текст отзыва:
                %s

                Принять отзыв:
                %s

                Отклонить отзыв:
                %s
                """.formatted(
                review.getRating(),
                review.getName(),
                review.getCarModel(),
                review.getCountry(),
                review.getText(),
                approveUrl,
                rejectUrl
        );

        sendMessage("Новый отзыв ожидает модерации", body, photos == null ? List.of() : photos, reviewRecipients());
    }

    private void sendMessage(
            String subject,
            String body,
            List<MultipartFile> attachments,
            List<String> recipients
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.getFrom());
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(body, false);

            for (MultipartFile attachment : attachments) {
                if (!attachment.isEmpty()) {
                    helper.addAttachment(attachment.getOriginalFilename(), attachment);
                }
            }

            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Не удалось подготовить письмо", exception);
        }
    }

    private List<String> leadRecipients() {
        List<String> recipients = properties.getLeadTo().stream()
                .filter(address -> address != null && !address.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return recipients.isEmpty() ? reviewRecipients() : recipients;
    }

    private List<String> reviewRecipients() {
        return List.of(properties.getTo());
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
