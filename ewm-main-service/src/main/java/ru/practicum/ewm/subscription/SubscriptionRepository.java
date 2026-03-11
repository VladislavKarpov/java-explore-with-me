package ru.practicum.ewm.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Subscription> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Subscription> findAllByFollowerId(Long followerId);

    List<Subscription> findAllByFollowingId(Long followingId);
}