package ru.practicum.ewm.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ModerationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModerationNoteRequest {
        @NotBlank
        @Size(min = 1, max = 1000)
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModerationNoteResponse {
        private Long id;
        private Long eventId;
        private Long adminId;
        private String note;
        private String createdOn;
    }
}