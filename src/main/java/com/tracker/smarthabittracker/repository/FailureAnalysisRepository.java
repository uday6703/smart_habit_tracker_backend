package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.FailureAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FailureAnalysisRepository extends JpaRepository<FailureAnalysis, Long> {
    List<FailureAnalysis> findByUserId(Long userId);
    Optional<FailureAnalysis> findByUserIdAndHabitId(Long userId, Long habitId);
    void deleteByUserId(Long userId);
}
