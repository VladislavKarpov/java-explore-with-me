package ru.practicum.ewm.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByEventId(Long eventId, Pageable pageable);
    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);
    Optional<Comment> findByIdAndAuthorId(Long id, Long authorId);
}