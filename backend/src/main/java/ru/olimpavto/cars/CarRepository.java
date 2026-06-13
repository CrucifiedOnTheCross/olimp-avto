package ru.olimpavto.cars;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CarRepository extends JpaRepository<CarEntity, Long> {

    @Query("select distinct c.country from CarEntity c order by c.country")
    List<String> findDistinctCountries();
}
