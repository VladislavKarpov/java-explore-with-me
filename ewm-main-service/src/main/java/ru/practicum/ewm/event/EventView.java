package ru.practicum.ewm.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "ip"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "ip", nullable = false, length = 50)
    private String ip;
}