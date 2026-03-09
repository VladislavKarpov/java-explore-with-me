package ru.practicum.ewm.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.EventState;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserService userService;

    @Transactional
    public CommentDto.CommentResponseDto addComment(Long userId, Long eventId, CommentDto.NewCommentDto dto) {
        User author = userService.getEntityById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }

        Comment comment = Comment.builder()
                .text(dto.getText())
                .event(event)
                .author(author)
                .createdOn(LocalDateTime.now()).build();

        return toDto(commentRepository.save(comment));
    }

    @Transactional
    public CommentDto.CommentResponseDto updateComment(Long userId, Long commentId, CommentDto.NewCommentDto dto) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        comment.setText(dto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        return toDto(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
        commentRepository.delete(comment);
    }

    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Comment with id=" + commentId + " was not found");
        }
        commentRepository.deleteById(commentId);
    }

    public List<CommentDto.CommentResponseDto> getEventComments(Long eventId, int from, int size) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return commentRepository.findAllByEventId(eventId,
                        PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "createdOn")))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<CommentDto.CommentResponseDto> getUserComments(Long userId, int from, int size) {
        userService.getEntityById(userId);
        return commentRepository.findAllByAuthorId(userId,
                        PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "createdOn")))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private CommentDto.CommentResponseDto toDto(Comment c) {
        return CommentDto.CommentResponseDto.builder()
                .id(c.getId())
                .text(c.getText())
                .eventId(c.getEvent().getId())
                .authorId(c.getAuthor().getId())
                .authorName(c.getAuthor().getName())
                .createdOn(c.getCreatedOn().format(FORMATTER))
                .updatedOn(c.getUpdatedOn() != null ? c.getUpdatedOn().format(FORMATTER) : null).build();
    }
}