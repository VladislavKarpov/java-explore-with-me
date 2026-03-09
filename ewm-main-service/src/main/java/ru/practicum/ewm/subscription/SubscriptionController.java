package ru.practicum.ewm.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.EventFullDto;
import ru.practicum.ewm.user.UserResponseDto;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping("/{followingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void subscribe(@PathVariable Long userId, @PathVariable Long followingId) {
        subscriptionService.subscribe(userId, followingId);
    }

    @DeleteMapping("/{followingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable Long userId, @PathVariable Long followingId) {
        subscriptionService.unsubscribe(userId, followingId);
    }

    @GetMapping("/followings")
    public List<UserResponseDto> getFollowings(@PathVariable Long userId) {
        return subscriptionService.getFollowings(userId);
    }

    @GetMapping("/followers")
    public List<UserResponseDto> getFollowers(@PathVariable Long userId) {
        return subscriptionService.getFollowers(userId);
    }

    @GetMapping("/events")
    public List<EventFullDto> getFollowingEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        return subscriptionService.getFollowingEvents(userId, from, size);
    }
}