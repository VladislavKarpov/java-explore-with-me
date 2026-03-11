package ru.practicum.ewm.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    @Query("SELECT l FROM Location l WHERE " +
            "SQRT(POWER(l.lat - :lat, 2) + POWER(l.lon - :lon, 2)) * 111 <= l.radius")
    List<Location> findByCoordinates(@Param("lat") Float lat, @Param("lon") Float lon);
}