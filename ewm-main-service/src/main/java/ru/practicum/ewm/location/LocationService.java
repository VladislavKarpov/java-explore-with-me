package ru.practicum.ewm.location;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {
    private final LocationRepository locationRepository;

    @Transactional
    public LocationDto.LocationResponseDto create(LocationDto.NewLocationDto dto) {
        Location location = Location.builder()
                .name(dto.getName())
                .lat(dto.getLat())
                .lon(dto.getLon())
                .radius(dto.getRadius()).build();
        return toDto(locationRepository.save(location));
    }

    @Transactional
    public LocationDto.LocationResponseDto update(Long id, LocationDto.NewLocationDto dto) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location with id=" + id + " was not found"));
        if (dto.getName() != null) location.setName(dto.getName());
        if (dto.getLat() != null) location.setLat(dto.getLat());
        if (dto.getLon() != null) location.setLon(dto.getLon());
        if (dto.getRadius() != null) location.setRadius(dto.getRadius());
        return toDto(locationRepository.save(location));
    }

    @Transactional
    public void delete(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new NotFoundException("Location with id=" + id + " was not found");
        }
        locationRepository.deleteById(id);
    }

    public List<LocationDto.LocationResponseDto> getAll() {
        return locationRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public LocationDto.LocationResponseDto getById(Long id) {
        return toDto(locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location with id=" + id + " was not found")));
    }

    public List<LocationDto.LocationResponseDto> findByCoordinates(Float lat, Float lon) {
        return locationRepository.findByCoordinates(lat, lon).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    private LocationDto.LocationResponseDto toDto(Location l) {
        return LocationDto.LocationResponseDto.builder()
                .id(l.getId())
                .name(l.getName())
                .lat(l.getLat())
                .lon(l.getLon())
                .radius(l.getRadius()).build();
    }
}