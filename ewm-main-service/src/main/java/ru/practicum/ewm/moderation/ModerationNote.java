package ru.practicum.ewm.moderation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false, length = 1000)
    private String note;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;
}