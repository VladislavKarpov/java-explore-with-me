package ru.practicum.ewm.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.EventState;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {
    private final EventRatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final UserService userService;

    @Transactional
    public RatingDto addLike(Long userId, Long eventId) {
        return addRating(userId, eventId, true);
    }

    @Transactional
    public RatingDto addDislike(Long userId, Long eventId) {
        return addRating(userId, eventId, false);
    }

    @Transactional
    public void removeRating(Long userId, Long eventId) {
        EventRating rating = ratingRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Rating not found"));
        ratingRepository.delete(rating);
    }

    public RatingDto getRating(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return buildDto(eventId);
    }

    private RatingDto addRating(Long userId, Long eventId, boolean liked) {
        User user = userService.getEntityById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot rate unpublished event");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Cannot rate your own event");
        }

        Optional<EventRating> existing = ratingRepository.findByEventIdAndUserId(eventId, userId);
        if (existing.isPresent()) {
            existing.get().setLiked(liked);
            ratingRepository.save(existing.get());
        } else {
            ratingRepository.save(EventRating.builder()
                    .event(event)
                    .user(user)
                    .liked(liked).build());
        }

        return buildDto(eventId);
    }

    private RatingDto buildDto(Long eventId) {
        long likes = ratingRepository.countLikes(eventId);
        long dislikes = ratingRepository.countDislikes(eventId);
        return RatingDto.builder()
                .eventId(eventId)
                .likes(likes)
                .dislikes(dislikes)
                .rating(likes - dislikes).build();
    }
}