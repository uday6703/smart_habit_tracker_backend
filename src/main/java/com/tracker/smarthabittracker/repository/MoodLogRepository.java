package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.MoodLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodLogRepository extends JpaRepository<MoodLog, Long> {
    List<MoodLog> findByUserIdOrderByLoggedDateAsc(Long userId);
    List<MoodLog> findByUserIdAndLoggedDateAfterOrderByLoggedDateAsc(Long userId, LocalDate date);
    Optional<MoodLog> findByUserIdAndLoggedDate(Long userId, LocalDate loggedDate);
    void deleteByUserId(Long userId);
}
