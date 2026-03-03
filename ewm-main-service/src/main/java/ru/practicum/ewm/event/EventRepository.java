package ru.practicum.ewm.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    boolean existsByCategoryId(Long categoryId);

    Optional<Event> findByIdAndState(Long id, EventState state);

    List<Event> findAllByIdIn(List<Long> ids);

    @Modifying
    @Query("UPDATE Event e SET e.views = e.views + 1 WHERE e.id = :id")
    void incrementViews(@Param("id") Long id);
}