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
            log.info("Saving hit - app: {}, uri: {}, ip: {}, time: {}", appName, uri, ip, LocalDateTime.now());
            statsClient.saveHit(appName, uri, ip, LocalDateTime.now());
            log.info("Hit saved successfully for uri: {}", uri);
        } catch (Exception e) {
            log.error("Failed to save hit stats: {}", e.getMessage(), e);
        }
    }

    public long getViews(String uri) {
        try {
            log.info("Getting views for uri: {}", uri);
            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(100);

            Thread.sleep(100);

            List<ViewStats> stats = statsClient.getStats(start, end, List.of(uri), true);

            if (stats != null && !stats.isEmpty()) {
                long views = stats.get(0).getHits();
                log.info("Views for uri {}: {}", uri, views);
                return views;
            }
            log.info("No stats found for uri: {}", uri);
            return 0L;
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            return 0L;
        }
    }

    public Map<Long, Long> getViewsMap(List<Long> eventIds) {
        Map<Long, Long> result = new HashMap<>();
        if (eventIds == null || eventIds.isEmpty()) {
            return result;
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            log.info("Getting views map for {} events", eventIds.size());

            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(100);

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            if (stats != null) {
                for (ViewStats vs : stats) {
                    String uri = vs.getUri();
                    try {
                        Long id = Long.parseLong(uri.replace("/events/", ""));
                        result.put(id, vs.getHits());
                        log.debug("Event {} has {} views", id, vs.getHits());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse event id from uri: {}", uri);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get stats map: {}", e.getMessage(), e);
        }

        log.info("Views map result size: {}", result.size());
        return result;
    }

    public long saveHitAndGetViews(String uri, String ip) {
        saveHit(uri, ip);
        return getViews(uri);
    }
}