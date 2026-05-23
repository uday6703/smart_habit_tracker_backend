package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.TinyHabitRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TinyHabitRecommendationRepository extends JpaRepository<TinyHabitRecommendation, Long> {
    List<TinyHabitRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<TinyHabitRecommendation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    Optional<TinyHabitRecommendation> findByIdAndUserId(Long id, Long userId);
    Optional<TinyHabitRecommendation> findByUserIdAndOriginalHabitIdAndStatus(Long userId, Long originalHabitId, String status);
}
