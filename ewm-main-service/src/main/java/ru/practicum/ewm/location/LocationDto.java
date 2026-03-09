package ru.practicum.ewm.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LocationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewLocationDto {
        @NotBlank
        private String name;
        @NotNull
        private Float lat;
        @NotNull
        private Float lon;
        @NotNull
        private Float radius;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationResponseDto {
        private Long id;
        private String name;
        private Float lat;
        private Float lon;
        private Float radius;
    }
}