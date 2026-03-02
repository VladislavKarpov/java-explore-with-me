package ru.practicum.ewm.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {
    private final StatsClient statsClient;

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    public void saveHit(String uri, String ip) {
        try {
            statsClient.saveHit(appName, uri, ip, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Failed to save hit stats for uri={}: {}", uri, e.getMessage());
        }
    }

    public long getViews(String uri) {
        try {
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now().plusYears(100),
                    List.of(uri),
                    true
            );
            if (stats != null && !stats.isEmpty()) {
                return stats.get(0).getHits();
            }
        } catch (Exception e) {
            log.warn("Failed to get stats for uri={}: {}", uri, e.getMessage());
        }
        return 0L;
    }

    public Map<Long, Long> getViewsMap(List<Long> eventIds) {
        Map<Long, Long> result = new HashMap<>();
        if (eventIds == null || eventIds.isEmpty()) return result;
        try {
            List<String> uris = eventIds.stream().map(id -> "/events/" + id).collect(Collectors.toList());
            List<ViewStats> stats = statsClient.getStats(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now().plusYears(100),
                    uris,
                    true
            );
            if (stats != null) {
                for (ViewStats vs : stats) {
                    try {
                        Long id = Long.parseLong(vs.getUri().replace("/events/", ""));
                        result.put(id, vs.getHits());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get stats map: {}", e.getMessage());
        }
        return result;
    }
}
