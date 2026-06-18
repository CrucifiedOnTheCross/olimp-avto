package ru.olimpavto.auctions;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.olimpavto.api.AuctionsApiDelegate;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.common.NotFoundException;
import ru.olimpavto.dto.AuctionLeadRequest;
import ru.olimpavto.dto.AuctionCaptchaRequest;
import ru.olimpavto.dto.AuctionLot;
import ru.olimpavto.dto.AuctionSearchResponse;
import ru.olimpavto.dto.ConsultationRequest;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.forms.LeadsController;

@Service
public class AuctionsController implements AuctionsApiDelegate {

    private final AuctionClient auctionClient;
    private final LeadsController leadsController;
    private final AuctionAccessGuard accessGuard;

    public AuctionsController(
            AuctionClient auctionClient,
            LeadsController leadsController,
            AuctionAccessGuard accessGuard
    ) {
        this.auctionClient = auctionClient;
        this.leadsController = leadsController;
        this.accessGuard = accessGuard;
    }

    @Override
    public ResponseEntity<AuctionSearchResponse> searchAuctions(
            String source,
            String query,
            String manufacturer,
            String model,
            Integer yearFrom,
            Integer yearTo,
            Integer maxMileage,
            String lotNumber,
            Integer dayOfWeek,
            Integer limit
    ) {
        validateSearch(yearFrom, yearTo, dayOfWeek, limit);
        return ResponseEntity.ok(auctionClient.search(new AuctionSearchCriteria(
                source(source),
                query,
                manufacturer,
                model,
                yearFrom,
                yearTo,
                maxMileage,
                lotNumber,
                dayOfWeek,
                limit
        )));
    }

    @Override
    public ResponseEntity<AuctionLot> getAuctionLot(String id, String source) {
        accessGuard.checkLotView();
        AuctionLot lot = auctionClient.getLot(source(source), id);
        if (lot == null) {
            throw new NotFoundException("Лот не найден");
        }
        return ResponseEntity.ok(lot);
    }

    @Override
    public ResponseEntity<java.util.List<String>> listAuctionManufacturers(String source) {
        return ResponseEntity.ok(auctionClient.manufacturers(source(source)));
    }

    @Override
    public ResponseEntity<java.util.List<String>> listAuctionModels(String source, String manufacturer) {
        if (manufacturer == null || manufacturer.isBlank()) {
            throw new BadRequestException("Не выбрана марка", Map.of("manufacturer", "Выберите марку"));
        }
        return ResponseEntity.ok(auctionClient.models(source(source), manufacturer.trim()));
    }

    @Override
    public ResponseEntity<FormResponse> verifyAuctionCaptcha(@Valid AuctionCaptchaRequest request) {
        accessGuard.verify(request.getCaptchaId(), request.getAnswer());
        return ResponseEntity.ok(new FormResponse("Проверка пройдена"));
    }

    @Override
    public ResponseEntity<FormResponse> sendAuctionLead(@Valid AuctionLeadRequest auctionLeadRequest) {
        if (!Boolean.TRUE.equals(auctionLeadRequest.getPolicyAccepted())) {
            throw new BadRequestException(
                    "Необходимо согласие на обработку персональных данных",
                    Map.of("policyAccepted", "Подтвердите согласие")
            );
        }

        ConsultationRequest request = new ConsultationRequest()
                .name(auctionLeadRequest.getName())
                .phone(auctionLeadRequest.getPhone())
                .comment(comment(auctionLeadRequest))
                .policyAccepted(true);

        leadsController.sendLead(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new FormResponse("Заявка по аукционному лоту отправлена"));
    }

    private void validateSearch(Integer yearFrom, Integer yearTo, Integer dayOfWeek, Integer limit) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            fields.put("yearFrom", "Год от не может быть больше года до");
        }
        if (limit != null && (limit < 1 || limit > 50)) {
            fields.put("limit", "Можно запросить от 1 до 50 лотов");
        }
        if (dayOfWeek != null && (dayOfWeek < 1 || dayOfWeek > 7)) {
            fields.put("dayOfWeek", "Выберите день недели");
        }
        if (!fields.isEmpty()) {
            throw new BadRequestException("Проверьте параметры поиска", fields);
        }
    }

    private AuctionSource source(String value) {
        try {
            return AuctionSource.fromApiValue(value);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "Неизвестный источник аукционов",
                    Map.of("source", "Выберите Японию, Корею или Китай")
            );
        }
    }

    private String comment(AuctionLeadRequest request) {
        return """
                Заявка по аукционному лоту
                Лот: %s
                Автомобиль: %s

                Комментарий клиента:
                %s
                """.formatted(
                request.getLotId(),
                valueOrDash(request.getLotTitle()),
                valueOrDash(request.getComment())
        );
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
