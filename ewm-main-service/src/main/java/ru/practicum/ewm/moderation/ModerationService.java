package ru.practicum.ewm.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventRepository;
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
public class ModerationService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ModerationNoteRepository noteRepository;
    private final EventRepository eventRepository;
    private final UserService userService;

    @Transactional
    public ModerationDto.ModerationNoteResponse addNote(Long adminId, Long eventId,
                                                        ModerationDto.ModerationNoteRequest dto) {
        User admin = userService.getEntityById(adminId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        ModerationNote note = ModerationNote.builder()
                .event(event)
                .admin(admin)
                .note(dto.getNote())
                .createdOn(LocalDateTime.now()).build();

        return toDto(noteRepository.save(note));
    }

    public List<ModerationDto.ModerationNoteResponse> getEventNotes(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return noteRepository.findAllByEventId(eventId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new NotFoundException("Note with id=" + noteId + " was not found");
        }
        noteRepository.deleteById(noteId);
    }

    private ModerationDto.ModerationNoteResponse toDto(ModerationNote n) {
        return ModerationDto.ModerationNoteResponse.builder()
                .id(n.getId())
                .eventId(n.getEvent().getId())
                .adminId(n.getAdmin().getId())
                .note(n.getNote())
                .createdOn(n.getCreatedOn().format(FORMATTER)).build();
    }
}