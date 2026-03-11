package ru.practicum.ewm.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewLocationDto {

    @NotBlank
    private String name;

    @NotNull
    private Float lat;

    @NotNull
    private Float lon;

    @NotNull
    private Float radius;
}