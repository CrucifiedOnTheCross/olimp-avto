package ru.olimpavto.leads;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<LeadEntity, Long> {

    List<LeadEntity> findAllByOrderByCreatedAtDesc();
}
