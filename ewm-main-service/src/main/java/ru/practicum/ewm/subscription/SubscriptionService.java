package ru.practicum.ewm.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.EventFullDto;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.event.EventState;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserResponseDto;
import ru.practicum.ewm.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final EventRepository eventRepository;
    private final EventService eventService;

    @Transactional
    public void subscribe(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new ConflictException("Cannot subscribe to yourself");
        }
        userService.getEntityById(followerId);
        userService.getEntityById(followingId);

        if (subscriptionRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new ConflictException("Already subscribed");
        }

        User follower = userService.getEntityById(followerId);
        User following = userService.getEntityById(followingId);

        subscriptionRepository.save(Subscription.builder()
                .follower(follower)
                .following(following).build());
    }

    @Transactional
    public void unsubscribe(Long followerId, Long followingId) {
        Subscription sub = subscriptionRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
        subscriptionRepository.delete(sub);
    }

    public List<UserResponseDto> getFollowings(Long userId) {
        userService.getEntityById(userId);
        return subscriptionRepository.findAllByFollowerId(userId).stream()
                .map(s -> userService.toDto(s.getFollowing()))
                .collect(Collectors.toList());
    }

    public List<UserResponseDto> getFollowers(Long userId) {
        userService.getEntityById(userId);
        return subscriptionRepository.findAllByFollowingId(userId).stream()
                .map(s -> userService.toDto(s.getFollower()))
                .collect(Collectors.toList());
    }

    public List<EventFullDto> getFollowingEvents(Long userId, int from, int size) {
        userService.getEntityById(userId);
        List<Long> followingIds = subscriptionRepository.findAllByFollowerId(userId).stream()
                .map(s -> s.getFollowing().getId())
                .collect(Collectors.toList());

        if (followingIds.isEmpty()) return List.of();

        return eventRepository.findAll(
                        (root, query, cb) -> cb.and(
                                root.get("initiator").get("id").in(followingIds),
                                cb.equal(root.get("state"), EventState.PUBLISHED)
                        ),
                        PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "eventDate"))
                ).stream()
                .map(e -> eventService.enrichFullDto(e))
                .collect(Collectors.toList());
    }
}