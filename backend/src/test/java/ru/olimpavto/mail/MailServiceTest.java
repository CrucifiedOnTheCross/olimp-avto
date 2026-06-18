package ru.olimpavto.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import ru.olimpavto.dto.ConsultationRequest;

class MailServiceTest {

    private JavaMailSender mailSender;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));

        MailProperties properties = new MailProperties();
        properties.setFrom("mail@olimpavtovl.ru");
        properties.setTo("olimpautovl125@gmail.com");
        properties.setLeadTo(List.of(
                "olimpautovl125@gmail.com",
                "olimpauvtovl125@outlook.com"
        ));
        mailService = new MailService(mailSender, properties);
    }

    @Test
    void leadGoesToBothBusinessMailboxes() throws Exception {
        mailService.sendLead(new ConsultationRequest()
                .name("Иван")
                .phone("+7 999 111-22-33")
                .policyAccepted(true));

        MimeMessage message = sentMessage();
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(Object::toString)
                .containsExactlyInAnyOrder(
                        "olimpautovl125@gmail.com",
                        "olimpauvtovl125@outlook.com"
                );
    }

    @Test
    void reviewGoesOnlyToModerationMailbox() throws Exception {
        mailService.sendReview(5, "Отличная работа", "Иван", "Toyota", "Япония", List.of());

        MimeMessage message = sentMessage();
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(Object::toString)
                .containsExactly("olimpautovl125@gmail.com");
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
