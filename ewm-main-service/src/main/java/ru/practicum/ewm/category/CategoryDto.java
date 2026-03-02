package ru.practicum.ewm.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CategoryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewCategoryDto {
        @NotBlank
        @Size(min = 1, max = 50)
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseCategoryDto {
        private Long id;
        @NotBlank
        @Size(min = 1, max = 50)
        private String name;
    }
}
