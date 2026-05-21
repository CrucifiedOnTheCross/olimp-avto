package ru.olimpavto.cars;

import org.springframework.stereotype.Component;
import ru.olimpavto.dto.CarRequest;
import ru.olimpavto.dto.CarResponse;

@Component
public class CarMapper {

    public CarEntity toEntity(CarRequest request) {
        CarEntity entity = new CarEntity();
        entity.setTitle(request.getTitle());
        entity.setCountry(request.getCountry());
        entity.setPrice(request.getPrice());
        entity.setYear(request.getYear());
        entity.setEngine(request.getEngine());
        entity.setDescription(request.getDescription());
        entity.setImageUrl(request.getImageUrl());
        return entity;
    }

    public CarResponse toResponse(CarEntity entity) {
        return new CarResponse()
                .id(entity.getId())
                .title(entity.getTitle())
                .country(entity.getCountry())
                .price(entity.getPrice())
                .year(entity.getYear())
                .engine(entity.getEngine())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl());
    }
}
