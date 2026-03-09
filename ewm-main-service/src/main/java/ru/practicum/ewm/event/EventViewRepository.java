package ru.practicum.ewm.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventViewRepository extends JpaRepository<EventView, Long> {
    boolean existsByEventIdAndIp(Long eventId, String ip);
}