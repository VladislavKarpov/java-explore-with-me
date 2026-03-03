package ru.practicum.ewm.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsClient statsClient;

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    public void saveHit(String uri, String ip) {
        try {
            statsClient.saveHit(appName, uri, ip, LocalDateTime.now());
        } catch (Throwable e) {
            log.warn("Failed to save hit for uri={}: {}", uri, e.getMessage());
        }
    }

    public long getViews(String uri) {
        try {
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now().plusSeconds(1),
                    List.of(uri),
                    true
            );
            if (stats != null && !stats.isEmpty()) {
                return stats.get(0).getHits();
            }
        } catch (Throwable e) {
            log.warn("Failed to get views for uri={}: {}", uri, e.getMessage());
        }
        return 0L;
    }

    public Map<Long, Long> getViewsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Collections.emptyMap();
        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now().plusSeconds(1),
                    uris,
                    true
            );
            Map<Long, Long> result = new HashMap<>();
            if (stats != null) {
                for (ViewStats stat : stats) {
                    try {
                        Long eventId = Long.parseLong(stat.getUri().replace("/events/", ""));
                        result.put(eventId, stat.getHits());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        } catch (Throwable e) {
            log.warn("Failed to get views map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}