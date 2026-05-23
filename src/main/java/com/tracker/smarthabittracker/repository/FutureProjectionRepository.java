package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.FutureProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FutureProjectionRepository extends JpaRepository<FutureProjection, Long> {
    List<FutureProjection> findByUserId(Long userId);
    Optional<FutureProjection> findByUserIdAndTimeHorizonDays(Long userId, Integer timeHorizonDays);
    void deleteByUserId(Long userId);
}
