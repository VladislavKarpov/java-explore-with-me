package ru.practicum.ewm.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.event.EventState;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {
    private final RequestRepository requestRepository;
    private final EventService eventService;
    private final UserService userService;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userService.getEntityById(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User user = userService.getEntityById(userId);
        Event event = eventService.getEntityById(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Event initiator cannot request participation in own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Duplicate request");
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        RequestStatus status = (!event.getRequestModeration() || event.getParticipantLimit() == 0)
                ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        ParticipationRequest req = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .status(status)
                .created(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)).build();

        return toDto(requestRepository.save(req));
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest req = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        req.setStatus(RequestStatus.CANCELED);
        return toDto(requestRepository.save(req));
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        userService.getEntityById(userId);
        Event event = eventService.getEntityById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only event initiator can view requests");
        }
        return requestRepository.findAllByEventId(eventId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest dto) {
        userService.getEntityById(userId);
        Event event = eventService.getEntityById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only event initiator can change request statuses");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(dto.getRequestIds());

        for (ParticipationRequest req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if ("CONFIRMED".equals(dto.getStatus())) {
            long alreadyConfirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            for (ParticipationRequest req : requests) {
                if (event.getParticipantLimit() > 0 && alreadyConfirmed >= event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(toDto(requestRepository.save(req)));
                } else {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(toDto(requestRepository.save(req)));
                    alreadyConfirmed++;
                }
            }
            if (event.getParticipantLimit() > 0 && alreadyConfirmed >= event.getParticipantLimit()) {
                requestRepository.findAllByEventId(eventId).stream()
                        .filter(r -> r.getStatus() == RequestStatus.PENDING)
                        .forEach(r -> {
                            r.setStatus(RequestStatus.REJECTED);
                            rejected.add(toDto(requestRepository.save(r)));
                        });
            }
        } else {
            for (ParticipationRequest req : requests) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(requestRepository.save(req)));
            }
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected).build();
    }

    private ParticipationRequestDto toDto(ParticipationRequest req) {
        return ParticipationRequestDto.builder()
                .id(req.getId())
                .event(req.getEvent().getId())
                .requester(req.getRequester().getId())
                .status(req.getStatus().name())
                .created(req.getCreated().truncatedTo(ChronoUnit.MICROS).toString()).build();
    }
}