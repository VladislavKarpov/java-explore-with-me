package ru.practicum.ewm.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.category.CategoryDto;
import ru.practicum.ewm.user.UserDto;

import java.time.LocalDateTime;

public class EventDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Float lat;
        private Float lon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewEventDto {
        @NotBlank
        @Size(min = 20, max = 2000)
        private String annotation;

        @NotNull
        private Long category;

        @NotBlank
        @Size(min = 20, max = 7000)
        private String description;

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime eventDate;

        @NotNull
        private Location location;

        private Boolean paid = false;

        @Min(0)
        private Integer participantLimit = 0;

        private Boolean requestModeration = true;

        @NotBlank
        @Size(min = 3, max = 120)
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventFullDto {
        private Long id;
        private String annotation;
        private CategoryDto.ResponseCategoryDto category;
        private Long confirmedRequests;
        private String createdOn;
        private String description;
        private String eventDate;
        private UserDto.UserShortDto initiator;
        private Location location;
        private Boolean paid;
        private Integer participantLimit;
        private String publishedOn;
        private Boolean requestModeration;
        private String state;
        private String title;
        private Long views;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventShortDto {
        private Long id;
        private String annotation;
        private CategoryDto.ResponseCategoryDto category;
        private Long confirmedRequests;
        private String eventDate;
        private UserDto.UserShortDto initiator;
        private Boolean paid;
        private String title;
        private Long views;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEventUserRequest {
        @Size(min = 20, max = 2000)
        private String annotation;
        private Long category;
        @Size(min = 20, max = 7000)
        private String description;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime eventDate;
        private Location location;
        private Boolean paid;
        @Min(0)
        private Integer participantLimit;
        private Boolean requestModeration;
        private String stateAction;
        @Size(min = 3, max = 120)
        private String title;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEventAdminRequest {
        @Size(min = 20, max = 2000)
        private String annotation;
        private Long category;
        @Size(min = 20, max = 7000)
        private String description;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime eventDate;
        private Location location;
        private Boolean paid;
        @Min(0)
        private Integer participantLimit;
        private Boolean requestModeration;
        private String stateAction;
        @Size(min = 3, max = 120)
        private String title;
    }
}
