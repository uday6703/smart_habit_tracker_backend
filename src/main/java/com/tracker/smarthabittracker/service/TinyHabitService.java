package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.TinyHabitDto;
import com.tracker.smarthabittracker.exception.ResourceNotFoundException;
import com.tracker.smarthabittracker.model.Habit;
import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.TinyHabitRecommendation;
import com.tracker.smarthabittracker.model.User;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import com.tracker.smarthabittracker.repository.TinyHabitRecommendationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TinyHabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final TinyHabitRecommendationRepository recommendationRepository;

    public TinyHabitService(HabitRepository habitRepository,
                            HabitLogRepository habitLogRepository,
                            TinyHabitRecommendationRepository recommendationRepository) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional(readOnly = true)
    public List<TinyHabitDto> getPendingRecommendations(User user) {
        // Automatically check and generate suggestions before returning
        generateRecommendationsForUser(user);
        
        List<TinyHabitRecommendation> list = recommendationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "PENDING");
        return list.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public void generateRecommendationsForUser(User user) {
        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        LocalDate today = LocalDate.now();

        for (Habit habit : habits) {
            // Only suggest for habits that are at least 5 days old
            long daysOld = ChronoUnit.DAYS.between(habit.getCreatedAt().toLocalDate(), today) + 1;
            if (daysOld < 5) continue;

            // Check if there is already a pending recommendation for this habit
            Optional<TinyHabitRecommendation> existing = recommendationRepository
                    .findByUserIdAndOriginalHabitIdAndStatus(user.getId(), habit.getId(), "PENDING");
            if (existing.isPresent()) continue;

            // Calculate completion rate in the last 7 days
            List<HabitLog> logs = habitLogRepository.findByHabitId(habit.getId());
            double completionRate = calculateCompletionRate(logs, habit.getCreatedAt().toLocalDate(), 7);

            // If completion rate is below 40%, trigger recommendation
            if (completionRate < 40.0) {
                String suggestedName = getSuggestedTinyName(habit);
                int suggestedTarget = Math.max(1, habit.getTargetCount() / 2);

                // If suggestion details would be identical to current, don't repeat recommendation
                if (habit.getName().equalsIgnoreCase(suggestedName) && habit.getTargetCount() <= suggestedTarget) {
                    continue;
                }

                TinyHabitRecommendation rec = TinyHabitRecommendation.builder()
                        .user(user)
                        .originalHabit(habit)
                        .suggestedName(suggestedName)
                        .suggestedTargetCount(suggestedTarget)
                        .status("PENDING")
                        .build();

                recommendationRepository.save(rec);
                log.info("Generated tiny habit recommendation for habit: {}", habit.getName());
            }
        }
    }

    @Transactional
    public TinyHabitDto acceptRecommendation(Long id, User user) {
        TinyHabitRecommendation rec = recommendationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found"));

        if (!rec.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Recommendation is already " + rec.getStatus());
        }

        // Apply changes to original habit
        Habit habit = rec.getOriginalHabit();
        habit.setName(rec.getSuggestedName());
        habit.setTargetCount(rec.getSuggestedTargetCount());
        habitRepository.save(habit);

        // Update recommendation status
        rec.setStatus("ACCEPTED");
        rec.setAdoptedAt(LocalDateTime.now());
        TinyHabitRecommendation saved = recommendationRepository.save(rec);

        return mapToDto(saved);
    }

    @Transactional
    public void dismissRecommendation(Long id, User user) {
        TinyHabitRecommendation rec = recommendationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found"));

        if (!rec.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Recommendation is already " + rec.getStatus());
        }

        rec.setStatus("DISMISSED");
        recommendationRepository.save(rec);
    }

    private double calculateCompletionRate(List<HabitLog> logs, LocalDate creationDate, int daysWindow) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(daysWindow - 1);
        LocalDate actualStart = creationDate.isAfter(start) ? creationDate : start;
        long totalDays = ChronoUnit.DAYS.between(actualStart, today) + 1;
        
        long completedDays = logs.stream()
                .map(HabitLog::getCompletedDate)
                .filter(d -> !d.isBefore(actualStart) && !d.isAfter(today))
                .distinct()
                .count();

        if (totalDays <= 0) return 0.0;
        return ((double) completedDays / totalDays) * 100.0;
    }

    private String getSuggestedTinyName(Habit habit) {
        String name = habit.getName().toLowerCase();
        if (name.contains("workout") || name.contains("gym") || name.contains("exercise") || name.contains("run")) {
            return "10 Min Quick Exercise";
        }
        if (name.contains("read") || name.contains("book")) {
            return "Read 2 Pages";
        }
        if (name.contains("code") || name.contains("program") || name.contains("study") || name.contains("learn")) {
            return "10 Min Focus Study";
        }
        if (name.contains("meditate") || name.contains("meditation")) {
            return "2 Min Breathing Exercise";
        }
        if (name.contains("water")) {
            return "Drink 1 Glass of Water";
        }
        if (name.contains("journal") || name.contains("write")) {
            return "Write 1 Sentence";
        }
        return "Tiny " + habit.getName();
    }

    private TinyHabitDto mapToDto(TinyHabitRecommendation rec) {
        return TinyHabitDto.builder()
                .id(rec.getId())
                .originalHabitId(rec.getOriginalHabit().getId())
                .originalHabitName(rec.getOriginalHabit().getName())
                .originalTargetCount(rec.getOriginalHabit().getTargetCount())
                .suggestedName(rec.getSuggestedName())
                .suggestedTargetCount(rec.getSuggestedTargetCount())
                .status(rec.getStatus())
                .createdAt(rec.getCreatedAt())
                .adoptedAt(rec.getAdoptedAt())
                .build();
    }
}
