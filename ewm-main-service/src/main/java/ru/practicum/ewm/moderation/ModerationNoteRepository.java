package ru.practicum.ewm.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationNoteRepository extends JpaRepository<ModerationNote, Long> {
    List<ModerationNote> findAllByEventId(Long eventId);
}