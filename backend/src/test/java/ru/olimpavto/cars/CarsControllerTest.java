package ru.olimpavto.cars;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.olimpavto.common.NotFoundException;
import ru.olimpavto.dto.CarRequest;
import ru.olimpavto.dto.CarResponse;

class CarsControllerTest {

    private final FakeCarRepository repository = new FakeCarRepository();
    private final CarsController controller = new CarsController(repository.proxy(), new CarMapper());

    @Test
    void createCarSavesEntityAndReturnsCreatedResponse() {
        CarRequest request = request();
        repository.nextId = 10L;

        ResponseEntity<CarResponse> response = controller.createCar(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(10L);
        assertThat(response.getBody().getTitle()).isEqualTo("Nissan Juke");
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void listCarsReturnsMappedRepositoryRows() {
        CarEntity car = entity(1L, "Nissan Juke");
        repository.rows.add(car);

        ResponseEntity<List<CarResponse>> response = controller.listCars();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getId()).isEqualTo(1L);
        assertThat(response.getBody().getFirst().getTitle()).isEqualTo("Nissan Juke");
    }

    @Test
    void getCarReturnsMappedCar() {
        repository.rows.add(entity(1L, "Nissan Juke"));

        ResponseEntity<CarResponse> response = controller.getCar(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void getCarThrowsNotFoundWhenRepositoryDoesNotHaveRow() {
        assertThatThrownBy(() -> controller.getCar(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Автомобиль не найден");
    }

    private CarRequest request() {
        return new CarRequest()
                .title("Nissan Juke")
                .country("Япония")
                .price(1_050_000L)
                .year(2015)
                .engine("1.5 л")
                .description("Компактный городской кроссовер")
                .imageUrl("images/car-1.jpg");
    }

    private CarEntity entity(Long id, String title) {
        CarEntity entity = new CarEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setCountry("Япония");
        entity.setPrice(1_050_000L);
        entity.setYear(2015);
        entity.setEngine("1.5 л");
        entity.setDescription("Компактный городской кроссовер");
        entity.setImageUrl("images/car-1.jpg");
        return entity;
    }

    private static class FakeCarRepository {

        private final List<CarEntity> rows = new ArrayList<>();
        private final List<CarEntity> saved = new ArrayList<>();
        private long nextId = 1L;

        CarRepository proxy() {
            return (CarRepository) Proxy.newProxyInstance(
                    CarRepository.class.getClassLoader(),
                    new Class<?>[] {CarRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "save" -> save((CarEntity) args[0]);
                        case "findAll" -> List.copyOf(rows);
                        case "findById" -> findById((Long) args[0]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private CarEntity save(CarEntity entity) {
            entity.setId(nextId++);
            saved.add(entity);
            rows.add(entity);
            return entity;
        }

        private Optional<CarEntity> findById(Long id) {
            return rows.stream()
                    .filter(row -> id.equals(row.getId()))
                    .findFirst();
        }
    }
}
