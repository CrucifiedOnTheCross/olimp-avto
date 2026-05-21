package ru.olimpavto.cars;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import ru.olimpavto.dto.CarRequest;
import ru.olimpavto.dto.CarResponse;

class CarMapperTest {

    private final CarMapper mapper = new CarMapper();

    @Test
    void mapsRequestToEntity() {
        CarRequest request = new CarRequest()
                .title("Nissan Juke")
                .country("Япония")
                .price(1_050_000L)
                .year(2015)
                .engine("1.5 л")
                .description("Компактный городской кроссовер")
                .imageUrl("images/car-1.jpg");

        CarEntity entity = mapper.toEntity(request);

        assertThat(entity.getTitle()).isEqualTo("Nissan Juke");
        assertThat(entity.getCountry()).isEqualTo("Япония");
        assertThat(entity.getPrice()).isEqualTo(1_050_000L);
        assertThat(entity.getYear()).isEqualTo(2015);
        assertThat(entity.getEngine()).isEqualTo("1.5 л");
        assertThat(entity.getDescription()).isEqualTo("Компактный городской кроссовер");
        assertThat(entity.getImageUrl()).isEqualTo("images/car-1.jpg");
    }

    @Test
    void mapsEntityToResponse() {
        CarEntity entity = new CarEntity();
        entity.setId(42L);
        entity.setTitle("Geely Monjaro");
        entity.setCountry("Китай");
        entity.setPrice(3_450_000L);
        entity.setYear(2023);
        entity.setEngine("2.0 л");
        entity.setDescription("Современный кроссовер");
        entity.setImageUrl("images/car-3.jpg");

        CarResponse response = mapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getTitle()).isEqualTo("Geely Monjaro");
        assertThat(response.getCountry()).isEqualTo("Китай");
        assertThat(response.getPrice()).isEqualTo(3_450_000L);
        assertThat(response.getYear()).isEqualTo(2023);
        assertThat(response.getEngine()).isEqualTo("2.0 л");
        assertThat(response.getDescription()).isEqualTo("Современный кроссовер");
        assertThat(response.getImageUrl()).isEqualTo("images/car-3.jpg");
    }
}
