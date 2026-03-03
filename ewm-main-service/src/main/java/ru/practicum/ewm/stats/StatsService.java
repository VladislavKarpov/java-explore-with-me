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

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsClient statsClient;

    @Value("${spring.application.name:ewm-main-service}")
    private String appName;

    public void saveHit(String uri, String ip) {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("Saving hit - app: {}, uri: {}, ip: {}", appName, uri, ip);
            statsClient.saveHit(appName, uri, ip, now);
            log.info("Hit saved successfully");
        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage());
        }
    }

    public long getViews(String uri) {
        try {
            log.info("Getting views for uri: {}", uri);
            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(100);

            List<ViewStats> stats = statsClient.getStats(start, end, List.of(uri), true);

            if (stats != null && !stats.isEmpty()) {
                long views = stats.get(0).getHits();
                log.info("Views for {}: {}", uri, views);
                return views;
            }
            log.info("No views found for {}", uri);
            return 0L;
        } catch (Exception e) {
            log.error("Error getting views: {}", e.getMessage());
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

            log.info("Getting views map for events: {}", eventIds);

            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(100);

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            if (stats != null) {
                for (ViewStats stat : stats) {
                    String uri = stat.getUri();
                    try {
                        Long eventId = Long.parseLong(uri.replace("/events/", ""));
                        result.put(eventId, stat.getHits());
                        log.info("Event {} has {} views", eventId, stat.getHits());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse event id from uri: {}", uri);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting views map: {}", e.getMessage());
        }

        return result;
    }
}