package ru.olimpavto.cars;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "cars")
public class CarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 80)
    private String country;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer year;

    @Column(length = 120)
    private String engine;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 500)
    private String imageUrl;
}
