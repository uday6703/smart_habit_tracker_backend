package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    List<HabitLog> findByHabitId(Long habitId);
    List<HabitLog> findByHabitIdAndCompletedDateBetween(Long habitId, LocalDate start, LocalDate end);
    Optional<HabitLog> findByHabitIdAndCompletedDate(Long habitId, LocalDate completedDate);
    List<HabitLog> findByHabitUserIdAndCompletedDateBetween(Long userId, LocalDate start, LocalDate end);
    List<HabitLog> findByHabitUserId(Long userId);
}
