package ru.practicum.ewm.moderation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ModerationController {
    private final ModerationService moderationService;

    @PostMapping("/users/{adminId}/events/{eventId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ModerationDto.ModerationNoteResponse addNote(
            @PathVariable Long adminId,
            @PathVariable Long eventId,
            @Valid @RequestBody ModerationDto.ModerationNoteRequest dto) {
        return moderationService.addNote(adminId, eventId, dto);
    }

    @GetMapping("/events/{eventId}/notes")
    public List<ModerationDto.ModerationNoteResponse> getEventNotes(@PathVariable Long eventId) {
        return moderationService.getEventNotes(eventId);
    }

    @DeleteMapping("/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable Long noteId) {
        moderationService.deleteNote(noteId);
    }
}