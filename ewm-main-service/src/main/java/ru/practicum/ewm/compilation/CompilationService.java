package ru.practicum.ewm.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.event.EventShortDto;
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
    public CompilationResponseDto create(NewCompilationDto dto) {
        Set<Event> events = new HashSet<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = new HashSet<>(eventService.findAllByIds(new ArrayList<>(dto.getEvents())));
        }
        Compilation compilation = Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .events(events).build();
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
    public CompilationResponseDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (dto.getTitle() != null) compilation.setTitle(dto.getTitle());
        if (dto.getPinned() != null) compilation.setPinned(dto.getPinned());
        if (dto.getEvents() != null) {
            compilation.setEvents(new HashSet<>(eventService.findAllByIds(new ArrayList<>(dto.getEvents()))));
        }

        return toDto(compilationRepository.save(compilation));
    }

    public List<CompilationResponseDto> getAll(Boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<Compilation> compilations = pinned != null
                ? compilationRepository.findAllByPinned(pinned, page).getContent()
                : compilationRepository.findAll(page).getContent();
        return compilations.stream().map(this::toDto).collect(Collectors.toList());
    }

    public CompilationResponseDto getById(Long compId) {
        return toDto(compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found")));
    }

    private CompilationResponseDto toDto(Compilation compilation) {
        List<EventShortDto> shortDtos = compilation.getEvents().isEmpty()
                ? Collections.emptyList()
                : compilation.getEvents().stream()
                .map(e -> eventService.toShortDto(e, 0L))
                .collect(Collectors.toList());

        return CompilationResponseDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(shortDtos).build();
    }
}