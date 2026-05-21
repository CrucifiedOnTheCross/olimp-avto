package ru.olimpavto.reviews;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findByStatusOrderByCreatedAtDesc(ReviewStatus status);

    Optional<ReviewEntity> findByIdAndModerationToken(Long id, String moderationToken);
}
