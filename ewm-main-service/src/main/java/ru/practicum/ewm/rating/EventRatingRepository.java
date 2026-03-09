package ru.practicum.ewm.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRatingRepository extends JpaRepository<EventRating, Long> {
    Optional<EventRating> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT COUNT(r) FROM EventRating r WHERE r.event.id = :eventId AND r.liked = true")
    long countLikes(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(r) FROM EventRating r WHERE r.event.id = :eventId AND r.liked = false")
    long countDislikes(@Param("eventId") Long eventId);
}