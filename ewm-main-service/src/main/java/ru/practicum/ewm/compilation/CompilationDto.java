package ru.practicum.ewm.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.EventDto;

import java.util.List;
import java.util.Set;

public class CompilationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewCompilationDto {
        private Set<Long> events;
        private Boolean pinned = false;

        @NotBlank
        @Size(min = 1, max = 50)
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCompilationRequest {
        private Set<Long> events;
        private Boolean pinned;

        @Size(min = 1, max = 50)
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseCompilationDto {
        private Long id;
        private String title;
        private Boolean pinned;
        private List<EventDto.EventShortDto> events;
    }
}
