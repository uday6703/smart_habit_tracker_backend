package com.tracker.smarthabittracker.repository;

import com.tracker.smarthabittracker.model.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {
    List<Suggestion> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Suggestion> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    Optional<Suggestion> findByIdAndUserId(Long id, Long userId);
    void deleteByUserIdAndType(Long userId, String type);
    void deleteByUserId(Long userId);
}
