package ru.olimpavto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.olimpavto.api.CarsApiDelegate;
import ru.olimpavto.api.LeadsApiDelegate;
import ru.olimpavto.api.ReviewsApiDelegate;
import ru.olimpavto.dto.CarResponse;
import ru.olimpavto.dto.FormResponse;

@WebMvcTest
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.admin-username=admin",
        "app.security.admin-password=admin123"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarsApiDelegate carsApiDelegate;

    @MockitoBean
    private LeadsApiDelegate leadsApiDelegate;

    @MockitoBean
    private ReviewsApiDelegate reviewsApiDelegate;

    @Test
    void publicEndpointsDoNotRequireAuthentication() throws Exception {
        when(leadsApiDelegate.sendLead(any())).thenReturn(org.springframework.http.ResponseEntity.accepted()
                .body(new FormResponse("Заявка отправлена")));

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Иван",
                                  "phone": "+7 999 111-22-33",
                                  "policyAccepted": true
                                }
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void catalogReadDoesNotRequireAuthentication() throws Exception {
        when(carsApiDelegate.listCars()).thenReturn(org.springframework.http.ResponseEntity.ok(java.util.List.of()));

        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk());
    }

    @Test
    void leadListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leadListAllowsAdminBasicAuth() throws Exception {
        when(leadsApiDelegate.listLeads()).thenReturn(org.springframework.http.ResponseEntity.ok(java.util.List.of()));

        mockMvc.perform(get("/api/leads").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void catalogCreateRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalogCreateAllowsAdminBasicAuth() throws Exception {
        when(carsApiDelegate.createCar(any())).thenReturn(org.springframework.http.ResponseEntity.status(201)
                .body(new CarResponse()
                        .id(1L)
                        .title("Toyota Camry")
                        .country("Япония")
                        .price(2_100_000L)
                        .year(2020)
                        .description("Седан в хорошем состоянии")));

        mockMvc.perform(post("/api/cars")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void swaggerRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerAllowsAdminBasicAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui.html").with(httpBasic("admin", "admin123")))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotIn(401, 403));
    }

    private String carJson() {
        return """
                {
                  "title": "Toyota Camry",
                  "country": "Япония",
                  "price": 2100000,
                  "year": 2020,
                  "description": "Седан в хорошем состоянии"
                }
                """;
    }
}
