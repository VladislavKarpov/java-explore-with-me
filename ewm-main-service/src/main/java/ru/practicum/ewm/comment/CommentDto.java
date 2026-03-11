package ru.practicum.ewm.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CommentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewCommentDto {
        @NotBlank
        @Size(min = 1, max = 2000)
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponseDto {
        private Long id;
        private String text;
        private Long eventId;
        private Long authorId;
        private String authorName;
        private String createdOn;
        private String updatedOn;
    }
}