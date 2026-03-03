package ru.practicum.ewm.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.CategoryService;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.RequestRepository;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.stats.StatsService;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserDto;
import ru.practicum.ewm.user.UserService;
import ru.practicum.ewm.category.CategoryDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final RequestRepository requestRepository;
    private final StatsService statsService;


    @Transactional
    public EventDto.EventFullDto createEvent(Long userId, EventDto.NewEventDto dto) {
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
                .createdOn(LocalDateTime.now())
                .build();

        return toFullDto(eventRepository.save(event), 0L, 0L);
    }

    public List<EventDto.EventShortDto> getUserEvents(Long userId, int from, int size) {
        userService.getEntityById(userId);
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageRequest).getContent();
        return enrichShortDtos(events);
    }

    public EventDto.EventFullDto getUserEvent(Long userId, Long eventId) {
        userService.getEntityById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return enrichFullDto(event);
    }

    @Transactional
    public EventDto.EventFullDto updateUserEvent(Long userId, Long eventId, EventDto.UpdateEventUserRequest dto) {
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


    public List<EventDto.EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                        String rangeStart, String rangeEnd, int from, int size) {
        List<EventState> stateList = states != null
                ? states.stream().map(EventState::valueOf).collect(Collectors.toList()) : null;
        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAll(
                EventSpecifications.adminFilter(users, stateList, categories, start, end),
                pageRequest
        ).getContent();

        return enrichFullDtos(events);
    }

    @Transactional
    public EventDto.EventFullDto updateEventByAdmin(Long eventId, EventDto.UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Event date must be at least 1 hour from publication time");
        }

        if ("PUBLISH_EVENT".equals(dto.getStateAction())) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
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

    public List<EventDto.EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                                        String rangeStart, String rangeEnd,
                                                        Boolean onlyAvailable, String sort,
                                                        int from, int size,
                                                        String ip, String uri) {
        // Сохраняем хит для поиска
        statsService.saveHit(uri, ip);

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        if (end != null && start.isAfter(end)) {
            throw new ValidationException("rangeStart must be before rangeEnd");
        }

        Sort sortOrder = "VIEWS".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "id") // Временная сортировка, потом пересортируем по views
                : Sort.by(Sort.Direction.ASC, "eventDate");

        PageRequest pageRequest = PageRequest.of(from / size, size, sortOrder);
        List<Event> events = eventRepository.findAll(
                EventSpecifications.publicFilter(text, categories, paid, start, end),
                pageRequest
        ).getContent();

        // Обогащаем данными
        List<EventDto.EventShortDto> result = enrichShortDtos(events);

        // Фильтруем только доступные
        if (Boolean.TRUE.equals(onlyAvailable)) {
            result = result.stream()
                    .filter(e -> {
                        Event ev = events.stream()
                                .filter(ev2 -> ev2.getId().equals(e.getId()))
                                .findFirst()
                                .orElse(null);
                        if (ev == null) return false;
                        return ev.getParticipantLimit() == 0 || e.getConfirmedRequests() < ev.getParticipantLimit();
                    })
                    .collect(Collectors.toList());
        }

        if ("VIEWS".equals(sort)) {
            result.sort((e1, e2) -> Long.compare(
                    Optional.ofNullable(e2.getViews()).orElse(0L),
                    Optional.ofNullable(e1.getViews()).orElse(0L)
            ));
        }

        return result;
    }

    public EventDto.EventFullDto getPublicEvent(Long id, String ip, String uri) {
        // 1. Сначала сохраняем хит в статистику (ВАЖНО: до получения события)
        statsService.saveHit(uri, ip);

        // 2. Затем получаем событие из БД
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        // 3. Получаем количество подтвержденных запросов
        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);

        // 4. Получаем количество просмотров (уже с учетом нового хита)
        long views = statsService.getViews(uri);

        log.debug("Event {}: confirmed={}, views={}", id, confirmed, views);

        // 5. Возвращаем полное DTO с актуальными данными
        return toFullDto(event, confirmed, views);
    }


    private void applyUpdateFields(Event event, String annotation, Long categoryId, String description,
                                   LocalDateTime eventDate, EventDto.Location location,
                                   Boolean paid, Integer participantLimit, Boolean requestModeration, String title) {
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

    public EventDto.EventFullDto enrichFullDto(Event event) {
        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = statsService.getViews("/events/" + event.getId());
        return toFullDto(event, confirmed, views);
    }

    private List<EventDto.EventFullDto> enrichFullDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedMap = getConfirmedMap(ids);
        Map<Long, Long> viewsMap = statsService.getViewsMap(ids);

        return events.stream()
                .map(e -> toFullDto(
                        e,
                        confirmedMap.getOrDefault(e.getId(), 0L),
                        viewsMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private List<EventDto.EventShortDto> enrichShortDtos(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();

        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedMap = getConfirmedMap(ids);
        Map<Long, Long> viewsMap = statsService.getViewsMap(ids);

        return events.stream()
                .map(e -> toShortDto(
                        e,
                        confirmedMap.getOrDefault(e.getId(), 0L),
                        viewsMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getConfirmedMap(List<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        List<Object[]> results = requestRepository.countConfirmedByEventIds(ids);
        for (Object[] row : results) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    public EventDto.EventFullDto toFullDto(Event e, long confirmed, long views) {
        return EventDto.EventFullDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(CategoryDto.ResponseCategoryDto.builder()
                        .id(e.getCategory().getId())
                        .name(e.getCategory().getName())
                        .build())
                .confirmedRequests(confirmed)
                .createdOn(e.getCreatedOn() != null ? e.getCreatedOn().format(FORMATTER) : null)
                .description(e.getDescription())
                .eventDate(e.getEventDate().format(FORMATTER))
                .initiator(UserDto.UserShortDto.builder()
                        .id(e.getInitiator().getId())
                        .name(e.getInitiator().getName())
                        .build())
                .location(new EventDto.Location(e.getLat(), e.getLon()))
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .publishedOn(e.getPublishedOn() != null ? e.getPublishedOn().format(FORMATTER) : null)
                .requestModeration(e.getRequestModeration())
                .state(e.getState().name())
                .title(e.getTitle())
                .views(views)
                .build();
    }

    public EventDto.EventShortDto toShortDto(Event e, long confirmed, long views) {
        return EventDto.EventShortDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .category(CategoryDto.ResponseCategoryDto.builder()
                        .id(e.getCategory().getId())
                        .name(e.getCategory().getName())
                        .build())
                .confirmedRequests(confirmed)
                .eventDate(e.getEventDate().format(FORMATTER))
                .initiator(UserDto.UserShortDto.builder()
                        .id(e.getInitiator().getId())
                        .name(e.getInitiator().getName())
                        .build())
                .paid(e.getPaid())
                .title(e.getTitle())
                .views(views)
                .build();
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