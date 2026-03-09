package ru.practicum.ewm.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @PutMapping("/users/{userId}/events/{eventId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingDto addLike(@PathVariable Long userId, @PathVariable Long eventId) {
        return ratingService.addLike(userId, eventId);
    }

    @PutMapping("/users/{userId}/events/{eventId}/dislike")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingDto addDislike(@PathVariable Long userId, @PathVariable Long eventId) {
        return ratingService.addDislike(userId, eventId);
    }

    @DeleteMapping("/users/{userId}/events/{eventId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRating(@PathVariable Long userId, @PathVariable Long eventId) {
        ratingService.removeRating(userId, eventId);
    }

    @GetMapping("/events/{eventId}/rating")
    public RatingDto getRating(@PathVariable Long eventId) {
        return ratingService.getRating(eventId);
    }
}