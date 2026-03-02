package ru.practicum.ewm.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewUserRequest {
        @NotBlank
        @Email
        @Size(min = 6, max = 254)
        private String email;

        @NotBlank
        @Size(min = 2, max = 250)
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseUserDto {
        private Long id;
        private String email;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserShortDto {
        private Long id;
        private String name;
    }
}
