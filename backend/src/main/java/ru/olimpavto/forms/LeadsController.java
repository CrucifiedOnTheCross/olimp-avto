package ru.olimpavto.forms;

import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.olimpavto.api.LeadsApiDelegate;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.ConsultationRequest;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.dto.LeadResponse;
import ru.olimpavto.leads.LeadEntity;
import ru.olimpavto.leads.LeadMapper;
import ru.olimpavto.leads.LeadRepository;
import ru.olimpavto.mail.MailService;

@Service
public class LeadsController implements LeadsApiDelegate {

    private final MailService mailService;
    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;

    public LeadsController(MailService mailService, LeadRepository leadRepository, LeadMapper leadMapper) {
        this.mailService = mailService;
        this.leadRepository = leadRepository;
        this.leadMapper = leadMapper;
    }

    @Override
    public ResponseEntity<FormResponse> sendLead(@Valid ConsultationRequest consultationRequest) {
        if (!Boolean.TRUE.equals(consultationRequest.getPolicyAccepted())) {
            throw new BadRequestException(
                    "Необходимо согласие на обработку персональных данных",
                    Map.of("policyAccepted", "Подтвердите согласие")
            );
        }

        leadRepository.save(toEntity(consultationRequest));
        mailService.sendLead(consultationRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new FormResponse("Заявка отправлена"));
    }

    @Override
    public ResponseEntity<List<LeadResponse>> listLeads() {
        return ResponseEntity.ok(leadRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(leadMapper::toResponse)
                .toList());
    }

    private LeadEntity toEntity(ConsultationRequest request) {
        LeadEntity entity = new LeadEntity();
        entity.setName(request.getName().trim());
        entity.setPhone(request.getPhone().trim());
        entity.setComment(request.getComment() == null || request.getComment().isBlank()
                ? null
                : request.getComment().trim());
        entity.setPolicyAccepted(Boolean.TRUE.equals(request.getPolicyAccepted()));
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }
}
