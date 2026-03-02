package ru.practicum.ewm.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.request.RequestDto;
import ru.practicum.ewm.request.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {
    private final EventService eventService;
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto.EventFullDto addEvent(@PathVariable Long userId,
                                          @Valid @RequestBody EventDto.NewEventDto dto) {
        return eventService.createEvent(userId, dto);
    }

    @GetMapping
    public List<EventDto.EventShortDto> getEvents(@PathVariable Long userId,
                                                  @RequestParam(defaultValue = "0") int from,
                                                  @RequestParam(defaultValue = "10") int size) {
        return eventService.getUserEvents(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventDto.EventFullDto getEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventService.getUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventDto.EventFullDto updateEvent(@PathVariable Long userId, @PathVariable Long eventId,
                                             @RequestBody EventDto.UpdateEventUserRequest dto) {
        return eventService.updateUserEvent(userId, eventId, dto);
    }

    @GetMapping("/{eventId}/requests")
    public List<RequestDto.ParticipationRequestDto> getEventParticipants(@PathVariable Long userId,
                                                                         @PathVariable Long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public RequestDto.EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId, @PathVariable Long eventId,
            @RequestBody RequestDto.EventRequestStatusUpdateRequest dto) {
        return requestService.updateRequestStatuses(userId, eventId, dto);
    }
}
