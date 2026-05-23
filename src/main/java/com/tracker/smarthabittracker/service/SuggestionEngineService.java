package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.model.*;
import com.tracker.smarthabittracker.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SuggestionEngineService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SuggestionRepository suggestionRepository;

    public SuggestionEngineService(HabitRepository habitRepository,
                                   HabitLogRepository habitLogRepository,
                                   SuggestionRepository suggestionRepository) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.suggestionRepository = suggestionRepository;
    }

    @Transactional
    public List<Suggestion> getActiveSuggestions(User user) {
        // Automatically regenerate suggestions to keep them fresh
        generateSuggestionsForUser(user);
        return suggestionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void generateSuggestionsForUser(User user) {
        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        if (habits.isEmpty()) return;

        // Clear existing suggestions to recalculate fresh recommendations
        suggestionRepository.deleteByUserIdAndType(user.getId(), "CONSISTENCY_DROP");
        suggestionRepository.deleteByUserIdAndType(user.getId(), "STREAK_WARNING");
        suggestionRepository.deleteByUserIdAndType(user.getId(), "TIME_ANALYSIS");
        suggestionRepository.deleteByUserIdAndType(user.getId(), "OVERLOAD");
        suggestionRepository.deleteByUserIdAndType(user.getId(), "MOOD_CORRELATION");
        suggestionRepository.deleteByUserIdAndType(user.getId(), "OPTIMIZATION");

        List<Suggestion> newSuggestions = new ArrayList<>();

        // Rule 4: Overload Warning (Overall active habits > 4 and low avg completion rate)
        double totalCompletionRateSum = 0.0;
        int activeHabitCount = habits.size();

        for (Habit habit : habits) {
            List<HabitLog> logs = habitLogRepository.findByHabitId(habit.getId());
            double compRate = calculateCompletionRate(logs, habit.getCreatedAt().toLocalDate());
            totalCompletionRateSum += compRate;

            // Evaluate per-habit rules
            evaluateConsistencyDrop(user, habit, logs, newSuggestions);
            evaluateStreakBreakWarning(user, habit, logs, newSuggestions);
            evaluateTimeOfDayPatterns(user, habit, logs, newSuggestions);
            evaluateMoodCorrelation(user, habit, logs, newSuggestions);
            evaluateTargetReduction(user, habit, logs, compRate, newSuggestions);
        }

        double averageCompletionRate = activeHabitCount > 0 ? (totalCompletionRateSum / activeHabitCount) : 0.0;
        if (activeHabitCount >= 5 && averageCompletionRate < 50.0) {
            newSuggestions.add(Suggestion.builder()
                    .user(user)
                    .content(String.format("You are tracking %d active habits, but your overall completion rate is only %.1f%%. You might be overloaded. Consider focusing on your top 3 habits to build consistency first.",
                            activeHabitCount, averageCompletionRate))
                    .type("OVERLOAD")
                    .isRead(false)
                    .build());
        }

        if (!newSuggestions.isEmpty()) {
            suggestionRepository.saveAll(newSuggestions);
        }
    }

    private void evaluateConsistencyDrop(User user, Habit habit, List<HabitLog> logs, List<Suggestion> suggestions) {
        if (logs.size() < 3) return;

        LocalDate today = LocalDate.now();
        LocalDate week1Start = today.minusDays(6);
        LocalDate week2Start = today.minusDays(13);
        LocalDate week2End = today.minusDays(7);

        long lastWeekCount = logs.stream()
                .filter(log -> !log.getCompletedDate().isBefore(week1Start) && !log.getCompletedDate().isAfter(today))
                .count();

        long previousWeekCount = logs.stream()
                .filter(log -> !log.getCompletedDate().isBefore(week2Start) && !log.getCompletedDate().isAfter(week2End))
                .count();

        if (previousWeekCount >= 3 && lastWeekCount < previousWeekCount) {
            double percentDrop = ((double) (previousWeekCount - lastWeekCount) / previousWeekCount) * 100.0;
            if (percentDrop >= 30.0) {
                suggestions.add(Suggestion.builder()
                        .user(user)
                        .habit(habit)
                        .content(String.format("Your consistency for '%s' dropped by %.0f%% this week compared to last week (from %d down to %d completions). Try logging it early today to rebuild your momentum!",
                                habit.getName(), percentDrop, previousWeekCount, lastWeekCount))
                        .type("CONSISTENCY_DROP")
                        .isRead(false)
                        .build());
            }
        }
    }

    private void evaluateStreakBreakWarning(User user, Habit habit, List<HabitLog> logs, List<Suggestion> suggestions) {
        if (logs.size() < 5) return;

        // Calculate current streak
        int currentStreak = calculateCurrentStreak(logs);
        if (currentStreak < 2) return;

        // Calculate average streak length before breaks
        List<LocalDate> sortedDates = logs.stream()
                .map(HabitLog::getCompletedDate)
                .distinct()
                .sorted()
                .toList();

        List<Integer> historicalStreaks = new ArrayList<>();
        int streakCount = 0;
        LocalDate prevDate = null;

        for (LocalDate date : sortedDates) {
            if (prevDate == null) {
                streakCount = 1;
            } else if (date.equals(prevDate.plusDays(1))) {
                streakCount++;
            } else {
                historicalStreaks.add(streakCount);
                streakCount = 1;
            }
            prevDate = date;
        }
        
        if (historicalStreaks.isEmpty()) return;
        double avgStreak = historicalStreaks.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int targetWarningDay = (int) Math.round(avgStreak);

        if (targetWarningDay >= 3 && currentStreak == targetWarningDay - 1) {
            suggestions.add(Suggestion.builder()
                    .user(user)
                    .habit(habit)
                    .content(String.format("Be careful! Historically, you tend to break your streak for '%s' around %d days. You are currently at day %d. Make sure to complete it today to break past your old limit!",
                            habit.getName(), targetWarningDay, currentStreak))
                    .type("STREAK_WARNING")
                    .isRead(false)
                    .build());
        }
    }

    private void evaluateTimeOfDayPatterns(User user, Habit habit, List<HabitLog> logs, List<Suggestion> suggestions) {
        if (logs.size() < 5) return;

        int morningCount = 0;   // 5am - 12pm
        int afternoonCount = 0; // 12pm - 5pm
        int eveningCount = 0;   // 5pm - 9pm
        int nightCount = 0;     // 9pm - 5am

        for (HabitLog log : logs) {
            LocalTime time = log.getCompletedTime();
            int hour = time.getHour();
            if (hour >= 5 && hour < 12) {
                morningCount++;
            } else if (hour >= 12 && hour < 17) {
                afternoonCount++;
            } else if (hour >= 17 && hour < 21) {
                eveningCount++;
            } else {
                nightCount++;
            }
        }

        int total = logs.size();
        String bestPeriod = "";
        int maxCount = 0;

        if (morningCount > maxCount) { maxCount = morningCount; bestPeriod = "Morning (5 AM - 12 PM)"; }
        if (afternoonCount > maxCount) { maxCount = afternoonCount; bestPeriod = "Afternoon (12 PM - 5 PM)"; }
        if (eveningCount > maxCount) { maxCount = eveningCount; bestPeriod = "Evening (5 PM - 9 PM)"; }
        if (nightCount > maxCount) { maxCount = nightCount; bestPeriod = "Night (9 PM - 5 AM)"; }

        double percentage = ((double) maxCount / total) * 100.0;
        if (percentage >= 70.0) {
            suggestions.add(Suggestion.builder()
                    .user(user)
                    .habit(habit)
                    .content(String.format("You are highly consistent completing '%s' in the %s (%.0f%% of your logs). Try aligning future productivity habits with this time frame!",
                            habit.getName(), bestPeriod, percentage))
                    .type("TIME_ANALYSIS")
                    .isRead(false)
                    .build());
        }
    }

    private void evaluateMoodCorrelation(User user, Habit habit, List<HabitLog> logs, List<Suggestion> suggestions) {
        long logsWithMood = logs.stream().filter(l -> l.getMood() != null).count();
        if (logsWithMood < 5) return;

        long stressSadCount = logs.stream()
                .filter(l -> l.getMood() != null && (l.getMood().equals("STRESSED") || l.getMood().equals("SAD")))
                .count();

        long happyEnergeticCount = logs.stream()
                .filter(l -> l.getMood() != null && (l.getMood().equals("HAPPY") || l.getMood().equals("ENERGETIC")))
                .count();

        // If completion is overwhelmingly associated with high mood, and missing/low with stress:
        if (happyEnergeticCount >= 4 && stressSadCount <= 1) {
            suggestions.add(Suggestion.builder()
                    .user(user)
                    .habit(habit)
                    .content(String.format("Your consistency for '%s' drops when you are feeling stressed or down. Try committing to a 'micro-version' (just 2 minutes) on busy days to keep the habit alive.",
                            habit.getName()))
                    .type("MOOD_CORRELATION")
                    .isRead(false)
                    .build());
        }
    }

    private void evaluateTargetReduction(User user, Habit habit, List<HabitLog> logs, double compRate, List<Suggestion> suggestions) {
        LocalDate today = LocalDate.now();
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(habit.getCreatedAt().toLocalDate(), today) + 1;
        
        if (daysSinceCreation >= 14 && compRate < 30.0) {
            suggestions.add(Suggestion.builder()
                    .user(user)
                    .habit(habit)
                    .content(String.format("Your 30-day completion rate for '%s' is low (%.1f%%). Consider reducing your target count or shifting it to a different category to rebuild your confidence.",
                            habit.getName(), compRate))
                    .type("OPTIMIZATION")
                    .isRead(false)
                    .build());
        }
    }

    private int calculateCurrentStreak(List<HabitLog> logs) {
        if (logs.isEmpty()) return 0;
        Set<LocalDate> dates = logs.stream()
                .map(HabitLog::getCompletedDate)
                .collect(Collectors.toSet());
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        if (!dates.contains(today) && !dates.contains(yesterday)) return 0;
        int streak = 0;
        LocalDate checkDate = dates.contains(today) ? today : yesterday;
        while (dates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }
        return streak;
    }

    private double calculateCompletionRate(List<HabitLog> logs, LocalDate creationDate) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(29);
        LocalDate actualStart = creationDate.isAfter(windowStart) ? creationDate : windowStart;
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(actualStart, today) + 1;
        long completedDays = logs.stream()
                .map(HabitLog::getCompletedDate)
                .filter(d -> !d.isBefore(actualStart) && !d.isAfter(today))
                .distinct()
                .count();
        if (totalDays <= 0) return 0.0;
        return ((double) completedDays / totalDays) * 100.0;
    }
}
