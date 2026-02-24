package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.model.Hit;
import ru.practicum.stats.server.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final HitRepository hitRepository;

    @Transactional
    public void saveHit(EndpointHit dto) {
        Hit hit = Hit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
                .build();
        hitRepository.save(hit);
    }

    @Transactional(readOnly = true)
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        List<String> uriFilter = (uris == null || uris.isEmpty()) ? null : uris;
        if (unique) {
            return hitRepository.findUniqueStats(start, end, uriFilter);
        }
        return hitRepository.findStats(start, end, uriFilter);
    }
}
