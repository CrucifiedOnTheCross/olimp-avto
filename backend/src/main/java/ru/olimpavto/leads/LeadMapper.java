package ru.olimpavto.leads;

import org.springframework.stereotype.Component;
import ru.olimpavto.dto.LeadResponse;

@Component
public class LeadMapper {

    public LeadResponse toResponse(LeadEntity entity) {
        return new LeadResponse()
                .id(entity.getId())
                .name(entity.getName())
                .phone(entity.getPhone())
                .comment(entity.getComment())
                .policyAccepted(entity.getPolicyAccepted())
                .createdAt(entity.getCreatedAt());
    }
}
