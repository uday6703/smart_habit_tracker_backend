package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUserId(Long userId);
    List<Habit> findByUserIdAndIsActiveTrue(Long userId);
    Optional<Habit> findByIdAndUserId(Long id, Long userId);
}
