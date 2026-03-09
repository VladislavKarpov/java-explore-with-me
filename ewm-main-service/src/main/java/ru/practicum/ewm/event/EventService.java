package ru.practicum.ewm.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.CategoryResponseDto;
import ru.practicum.ewm.category.CategoryService;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.RequestRepository;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.stats.StatsService;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserShortDto;
import ru.practicum.ewm.user.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final EventViewRepository eventViewRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final RequestRepository requestRepository;
    private final StatsService statsService;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User user = userService.getEntityById(userId);
        Category category = categoryService.getEntityById(dto.getCategory());

        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours from now");
        }

        Event event = Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .lat(dto.getLocation().getLat())
                .lon(dto.getLocation().getLon())
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .title(dto.getTitle())
                .initiator(user)
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now()).build();

        return toFullDto(eventRepository.save(event), 0L);
    }

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        userService.getEntityById(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId,
                PageRequest.of(from / size, size)).getContent();
        return enrichShortDtos(events);
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        userService.getEntityById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return enrichFullDto(event);
    }

    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        userService.getEntityById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours from now");
        }

        applyUpdateFields(event, dto.getAnnotation(), dto.getCategory(), dto.getDescription(),
                dto.getEventDate(), dto.getLocation(), dto.getPaid(), dto.getParticipantLimit(),
                dto.getRequestModeration(), dto.getTitle());

        if ("SEND_TO_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.PENDING);
        } else if ("CANCEL_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.CANCELED);
        }

        return enrichFullDto(eventRepository.save(event));
    }

    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states,
                                               List<Long> categories, String rangeStart,
                                               String rangeEnd, int from, int size) {
        List<EventState> stateList = states != null
                ? states.stream().map(EventState::valueOf).collect(Collectors.toList()) : null;
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        List<Event> events = eventRepository.findAll(
                EventSpecifications.adminFilter(users, stateList, categories, start, end),
                PageRequest.of(from / size, size)
        ).getContent();
        return enrichFullDtos(events);
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Event date must be at least 1 hour from publication time");
        }

        if ("PUBLISH_EVENT".equals(dto.getStateAction())) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: "
                        + event.getState());
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if ("REJECT_EVENT".equals(dto.getStateAction())) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject published event");
            }
            event.setState(EventState.CANCELED);
        }

        applyUpdateFields(event, dto.getAnnotation(), dto.getCategory(), dto.getDescription(),
                dto.getEventDate(), dto.getLocation(), dto.getPaid(), dto.getParticipantLimit(),
                dto.getRequestModeration(), dto.getTitle());

        return enrichFullDto(eventRepository.save(event));
    }

    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               int from, int size, String ip, String uri) {
        statsService.saveHit(uri, ip);

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        if (end != null && start.isAfter(end)) {
            throw new ValidationException("rangeStart must be before rangeEnd");
        }

        Sort sortOrder = "VIEWS".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "views")
                : Sort.by(Sort.Direction.ASC, "eventDate");

        List<Event> events = eventRepository.findAll(
                EventSpecifications.publicFilter(text, categories, paid, start, end),
                PageRequest.of(from / size, size, sortOrder)
        ).getContent();

        List<EventShortDto> result = enrichShortDtos(events);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            result = result.stream()
                    .filter(e -> {
                        Event ev = events.stream()
                                .filter(ev2 -> ev2.getId().equals(e.getId()))
                                .findFirst().orElse(null);
                        if (ev == null) return false;
                        return ev.getParticipantLimit() == 0
                                || e.getConfirmedRequests() < ev.getParticipantLimit();
                    })
                    .collect(Collectors.toList());
        }

        if ("VIEWS".equals(sort)) {
            result.sort((e1, e2) -> Long.compare(
                    Optional.ofNullable(e2.getViews()).orElse(0L),
                    Optional.ofNullable(e1.getViews()).orElse(0L)));
        }

        return result;
    }

    @Transactional
    public EventFullDto getPublicEvent(Long id, String ip, String uri) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        if (!eventViewRepository.existsByEventIdAndIp(id, ip)) {
            eventViewRepository.save(EventView.builder().eventId(id).ip(ip).build());
            eventRepository.incrementViews(id);
            event = eventRepository.findById(id).orElse(event);
        }

        statsService.saveHit(uri, ip);

        long confirmed = requestRepository.countByEventIdAndStatus(id, RequestStatus.CONFIRMED);
        return toFullDto(event, confirmed);
    }

    private void applyUpdateFields(Event event, String annotation, Long categoryId, String description,
                                   LocalDateTime eventDate, EventLocation location,
                                   Boolean paid, Integer participantLimit,
                                   Boolean requestModeration, String title) {
        if (annotation != null) event.setAnnotation(annotation);
        if (categoryId != null) event.setCategory(categoryService.getEntityById(categoryId));
        if (description != null) event.setDescription(description);
        if (eventDate != null) event.setEventDate(eventDate);
        if (location != null) {
            event.setLat(location.getLat());
            event.setLon(location.getLon());
        }
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setRequestModeration(requestModeration);
        if (title != null) event.setTitle(title);
    }

    public EventFullDto enrichFullDto(Event event) {
        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        return toFullDto(event, confirmed);
    }

    private List<EventFullDto> enrichFullDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedMap = getConfirmedMap(ids);
        return events.stream()
                .map(e -> toFullDto(e, confirmedMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private List<EventShortDto> enrichShortDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedMap = getConfirmedMap(ids);
        return events.stream()
                .map(e -> toShortDto(e, confirmedMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getConfirmedMap(List<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        requestRepository.countConfirmedByEventIds(ids)
                .forEach(row -> map.put((Long) row[0], (Long) row[1]));
        return map;
    }

    public EventFullDto toFullDto(Event e, long confirmed) {
        return EventFullDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(CategoryResponseDto.builder()
                        .id(e.getCategory().getId())
                        .name(e.getCategory().getName()).build())
                .confirmedRequests(confirmed)
                .createdOn(e.getCreatedOn() != null ? e.getCreatedOn().format(FORMATTER) : null)
                .description(e.getDescription())
                .eventDate(e.getEventDate().format(FORMATTER))
                .initiator(UserShortDto.builder()
                        .id(e.getInitiator().getId())
                        .name(e.getInitiator().getName()).build())
                .location(new EventLocation(e.getLat(), e.getLon()))
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .publishedOn(e.getPublishedOn() != null ? e.getPublishedOn().format(FORMATTER) : null)
                .requestModeration(e.getRequestModeration())
                .state(e.getState().name())
                .title(e.getTitle())
                .views(e.getViews()).build();
    }

    public EventShortDto toShortDto(Event e, long confirmed) {
        return EventShortDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(CategoryResponseDto.builder()
                        .id(e.getCategory().getId())
                        .name(e.getCategory().getName()).build())
                .confirmedRequests(confirmed)
                .eventDate(e.getEventDate().format(FORMATTER))
                .initiator(UserShortDto.builder()
                        .id(e.getInitiator().getId())
                        .name(e.getInitiator().getName()).build())
                .paid(e.getPaid())
                .title(e.getTitle())
                .views(e.getViews()).build();
    }

    public Event getEntityById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    public List<Event> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return eventRepository.findAllByIdIn(ids);
    }
}