package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.LifeBalanceScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LifeBalanceScoreRepository extends JpaRepository<LifeBalanceScore, Long> {
    List<LifeBalanceScore> findByUserIdOrderByRecordedDateAsc(Long userId);
    Optional<LifeBalanceScore> findFirstByUserIdOrderByRecordedDateDesc(Long userId);
    Optional<LifeBalanceScore> findByUserIdAndRecordedDate(Long userId, LocalDate recordedDate);
    void deleteByUserId(Long userId);
}
