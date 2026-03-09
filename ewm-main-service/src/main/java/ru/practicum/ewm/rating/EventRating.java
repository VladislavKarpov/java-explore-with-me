package ru.practicum.ewm.rating;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.user.User;

@Entity
@Table(name = "event_ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Boolean liked;
}