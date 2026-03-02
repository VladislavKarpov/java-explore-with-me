package ru.practicum.ewm.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    @Query("SELECT r.event.id, COUNT(r) FROM ParticipationRequest r WHERE r.event.id IN :eventIds AND r.status = 'CONFIRMED' GROUP BY r.event.id")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);

    List<ParticipationRequest> findAllByIdIn(List<Long> ids);
}
