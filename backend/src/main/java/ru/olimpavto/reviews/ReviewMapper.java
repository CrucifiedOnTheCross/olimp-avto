package ru.olimpavto.reviews;

import org.springframework.stereotype.Component;
import ru.olimpavto.dto.ReviewResponse;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(ReviewEntity entity) {
        return new ReviewResponse()
                .id(entity.getId())
                .rating(entity.getRating())
                .text(entity.getText())
                .name(entity.getName())
                .carModel(entity.getCarModel())
                .country(ReviewResponse.CountryEnum.fromValue(entity.getCountry()));
    }
}
