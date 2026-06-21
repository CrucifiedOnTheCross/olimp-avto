package ru.olimpavto.auctions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ru.olimpavto.common.BadRequestException;
import ru.olimpavto.dto.AuctionLeadRequest;
import ru.olimpavto.dto.FormResponse;
import ru.olimpavto.dto.AuctionSearchResponse;
import ru.olimpavto.forms.LeadsController;

class AuctionsControllerTest {

    private final RecordingLeadsController leadsController = new RecordingLeadsController();
    private final AuctionsController controller = new AuctionsController(null, leadsController, null);

    @Test
    void sendAuctionLeadStoresLeadWithLotContext() {
        AuctionLeadRequest request = new AuctionLeadRequest()
                .lotId("12345")
                .lotTitle("Toyota Prius 2022")
                .name("Иван")
                .phone("+7 999 111-22-33")
                .comment("Интересует расчет")
                .policyAccepted(true);

        ResponseEntity<FormResponse> response = controller.sendAuctionLead(request);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(leadsController.name).isEqualTo("Иван");
        assertThat(leadsController.phone).isEqualTo("+7 999 111-22-33");
        assertThat(leadsController.comment).contains("12345", "Toyota Prius 2022", "Интересует расчет");
    }

    @Test
    void sendAuctionLeadRequiresPolicyConsent() {
        AuctionLeadRequest request = new AuctionLeadRequest()
                .lotId("12345")
                .name("Иван")
                .phone("+7 999 111-22-33")
                .policyAccepted(false);

        assertThatThrownBy(() -> controller.sendAuctionLead(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Необходимо согласие на обработку персональных данных");
    }

    @Test
    void searchRejectsInvalidYearRange() {
        AuctionsController searchController = new AuctionsController(
                new FakeAuctionClient(),
                leadsController,
                new AuctionAccessGuard(new AuctionProperties())
        );

        assertThatThrownBy(() -> searchController.searchAuctions(
                "japan", null, null, null, 2025, 2020,
                null, null, null, null, null, null,
                null, null, null, null, null, 20, 0
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Проверьте параметры поиска");
    }

    @Test
    void modelDictionaryUsesGeneratedOpenApiParameterOrder() {
        RecordingAuctionClient client = new RecordingAuctionClient();
        AuctionsController searchController = new AuctionsController(
                client,
                leadsController,
                new AuctionAccessGuard(new AuctionProperties())
        );

        searchController.listAuctionModels("TOYOTA", "japan");

        assertThat(client.source).isEqualTo(AuctionSource.JAPAN);
        assertThat(client.manufacturer).isEqualTo("TOYOTA");
    }

    private static class RecordingLeadsController extends LeadsController {

        private String name;
        private String phone;
        private String comment;

        RecordingLeadsController() {
            super(null, null, null);
        }

        @Override
        public ResponseEntity<FormResponse> sendLead(ru.olimpavto.dto.ConsultationRequest request) {
            this.name = request.getName();
            this.phone = request.getPhone();
            this.comment = request.getComment();
            return ResponseEntity.accepted().body(new FormResponse("Заявка отправлена"));
        }
    }

    private static class FakeAuctionClient extends AuctionClient {

        FakeAuctionClient() {
            super(new AuctionProperties(), new AuctionSqlBuilder(), new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Override
        public AuctionSearchResponse search(AuctionSearchCriteria criteria) {
            return new AuctionSearchResponse().items(java.util.List.of());
        }
    }

    private static class RecordingAuctionClient extends FakeAuctionClient {

        private AuctionSource source;
        private String manufacturer;

        @Override
        public java.util.List<String> models(AuctionSource source, String manufacturer) {
            this.source = source;
            this.manufacturer = manufacturer;
            return java.util.List.of("PRIUS");
        }
    }
}
