package ru.olimpavto.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.ConsultationRequest;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.mail.MailService;

class LeadsControllerTest {

    private final RecordingMailService mailService = new RecordingMailService();
    private final LeadsController controller = new LeadsController(mailService);

    @Test
    void sendLeadSendsMailAndReturnsAccepted() {
        ConsultationRequest request = new ConsultationRequest()
                .name("Иван")
                .phone("+7 999 111-22-33")
                .comment("Хочу расчет")
                .policyAccepted(true);

        ResponseEntity<FormResponse> response = controller.sendLead(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Заявка отправлена");
        assertThat(mailService.leadRequest).isSameAs(request);
    }

    @Test
    void sendLeadRejectsMissingPolicyConsent() {
        ConsultationRequest request = new ConsultationRequest()
                .name("Иван")
                .phone("+7 999 111-22-33")
                .policyAccepted(false);

        assertThatThrownBy(() -> controller.sendLead(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Необходимо согласие на обработку персональных данных")
                .extracting("fields")
                .satisfies(fields -> assertThat(fields)
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("policyAccepted", "Подтвердите согласие"));

        assertThat(mailService.leadRequest).isNull();
    }

    private static class RecordingMailService extends MailService {

        private ConsultationRequest leadRequest;

        RecordingMailService() {
            super(null, null);
        }

        @Override
        public void sendLead(ConsultationRequest request) {
            this.leadRequest = request;
        }
    }
}
