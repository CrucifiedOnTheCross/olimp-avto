package ru.olimpavto.cars;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.olimpavto.api.CarsApiDelegate;
import ru.olimpavto.common.NotFoundException;
import ru.olimpavto.dto.CarFiltersResponse;
import ru.olimpavto.dto.CarRequest;
import ru.olimpavto.dto.CarResponse;

@Service
public class CarsController implements CarsApiDelegate {

    private final CarRepository repository;
    private final CarMapper mapper;

    public CarsController(CarRepository repository, CarMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<CarResponse> createCar(@Valid CarRequest carRequest) {
        CarEntity saved = repository.save(mapper.toEntity(carRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<CarResponse> getCar(Long id) {
        CarEntity car = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Автомобиль не найден"));
        return ResponseEntity.ok(mapper.toResponse(car));
    }

    @Override
    public ResponseEntity<List<CarResponse>> listCars() {
        return ResponseEntity.ok(repository.findAll().stream()
                .map(mapper::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<CarFiltersResponse> listCarFilters() {
        List<String> manufacturers = repository.findAll().stream()
                .map(CarEntity::getTitle)
                .map(this::manufacturer)
                .distinct()
                .sorted()
                .toList();

        return ResponseEntity.ok(new CarFiltersResponse()
                .countries(repository.findDistinctCountries())
                .manufacturers(manufacturers));
    }

    private String manufacturer(String title) {
        if (title == null || title.isBlank()) {
            return "Другое";
        }
        return title.trim().split("\\s+")[0];
    }
}
