package ru.practicum.ewm.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventDto;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventService eventService;

    @Transactional
    public CompilationDto.ResponseCompilationDto create(CompilationDto.NewCompilationDto dto) {
        Set<Event> events = new HashSet<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = new HashSet<>(eventService.findAllByIds(new ArrayList<>(dto.getEvents())));
        }
        Compilation compilation = Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .events(events)
                .build();
        return toDto(compilationRepository.save(compilation));
    }

    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Transactional
    public CompilationDto.ResponseCompilationDto update(Long compId, CompilationDto.UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (dto.getTitle() != null) compilation.setTitle(dto.getTitle());
        if (dto.getPinned() != null) compilation.setPinned(dto.getPinned());
        if (dto.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventService.findAllByIds(new ArrayList<>(dto.getEvents())));
            compilation.setEvents(events);
        }

        return toDto(compilationRepository.save(compilation));
    }

    public List<CompilationDto.ResponseCompilationDto> getAll(Boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<Compilation> compilations = pinned != null
                ? compilationRepository.findAllByPinned(pinned, page).getContent()
                : compilationRepository.findAll(page).getContent();
        return compilations.stream().map(this::toDto).collect(Collectors.toList());
    }

    public CompilationDto.ResponseCompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return toDto(compilation);
    }

    private CompilationDto.ResponseCompilationDto toDto(Compilation compilation) {
        List<Long> eventIds = compilation.getEvents().stream().map(Event::getId).collect(Collectors.toList());

        List<EventDto.EventShortDto> shortDtos = eventIds.isEmpty()
                ? Collections.emptyList()
                : compilation.getEvents().stream()
                .map(e -> eventService.toShortDto(e, 0L, 0L))
                .collect(Collectors.toList());

        return CompilationDto.ResponseCompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(shortDtos)
                .build();
    }
}
