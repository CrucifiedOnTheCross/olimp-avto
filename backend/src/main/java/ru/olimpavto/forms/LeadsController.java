package ru.olimpavto.forms;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.olimpavto.api.LeadsApiDelegate;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.ConsultationRequest;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.mail.MailService;

@Service
public class LeadsController implements LeadsApiDelegate {

    private final MailService mailService;

    public LeadsController(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public ResponseEntity<FormResponse> sendLead(@Valid ConsultationRequest consultationRequest) {
        if (!Boolean.TRUE.equals(consultationRequest.getPolicyAccepted())) {
            throw new BadRequestException(
                    "Необходимо согласие на обработку персональных данных",
                    Map.of("policyAccepted", "Подтвердите согласие")
            );
        }

        mailService.sendLead(consultationRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new FormResponse("Заявка отправлена"));
    }
}
