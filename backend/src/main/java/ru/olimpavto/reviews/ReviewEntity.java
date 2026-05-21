package ru.olimpavto.reviews;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "review_text", nullable = false, length = 2000)
    private String text;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String name;

    @Column(name = "car_model", nullable = false, length = 120)
    private String carModel;

    @Column(nullable = false, length = 40)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    @Column(nullable = false, unique = true, length = 80)
    private String moderationToken;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime moderatedAt;
}
