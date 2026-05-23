package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.HabitDnaProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HabitDnaProfileRepository extends JpaRepository<HabitDnaProfile, Long> {
    Optional<HabitDnaProfile> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
