package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.HabitStack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HabitStackRepository extends JpaRepository<HabitStack, Long> {
    List<HabitStack> findByUserId(Long userId);
    Optional<HabitStack> findByIdAndUserId(Long id, Long userId);
    List<HabitStack> findByCueHabitId(Long cueHabitId);
    List<HabitStack> findByTargetHabitId(Long targetHabitId);
    Optional<HabitStack> findByUserIdAndCueHabitId(Long userId, Long cueHabitId);
}
