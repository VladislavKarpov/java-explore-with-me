package ru.practicum.ewm.compilation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.EventShortDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationResponseDto {
    private Long id;
    private String title;
    private Boolean pinned;
    private List<EventShortDto> events;
}