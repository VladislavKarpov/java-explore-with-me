package ru.practicum.ewm.location;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @PostMapping("/admin/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto.LocationResponseDto create(@Valid @RequestBody LocationDto.NewLocationDto dto) {
        return locationService.create(dto);
    }

    @PatchMapping("/admin/locations/{locId}")
    public LocationDto.LocationResponseDto update(@PathVariable Long locId,
                                                  @RequestBody LocationDto.NewLocationDto dto) {
        return locationService.update(locId, dto);
    }

    @DeleteMapping("/admin/locations/{locId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long locId) {
        locationService.delete(locId);
    }

    @GetMapping("/admin/locations")
    public List<LocationDto.LocationResponseDto> getAll() {
        return locationService.getAll();
    }

    @GetMapping("/admin/locations/{locId}")
    public LocationDto.LocationResponseDto getById(@PathVariable Long locId) {
        return locationService.getById(locId);
    }

    @GetMapping("/locations")
    public List<LocationDto.LocationResponseDto> findByCoordinates(
            @RequestParam Float lat,
            @RequestParam Float lon) {
        return locationService.findByCoordinates(lat, lon);
    }
}