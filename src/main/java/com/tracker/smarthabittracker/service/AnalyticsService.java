package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.AnalyticsDto;
import com.tracker.smarthabittracker.dto.HabitDto;
import com.tracker.smarthabittracker.model.*;
import com.tracker.smarthabittracker.repository.HabitLogRepository;
import com.tracker.smarthabittracker.repository.HabitRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitService habitService;

    public AnalyticsService(HabitRepository habitRepository,
                            HabitLogRepository habitLogRepository,
                            HabitService habitService) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.habitService = habitService;
    }

    public AnalyticsDto getAnalyticsForUser(User user) {
        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<HabitLog> allLogs = habitLogRepository.findByHabitUserId(user.getId());
        
        List<HabitDto> habitDtos = habits.stream()
                .map(habitService::mapToDto)
                .toList();

        // 1. Overall Consistency Score (average of active habit completion rates)
        double overallConsistency = habitDtos.stream()
                .mapToDouble(HabitDto::getCompletionRate)
                .average()
                .orElse(0.0);
        overallConsistency = Math.round(overallConsistency * 10.0) / 10.0;

        // 2. Productivity Score (compositions of streak performance and recent completions)
        double last7DaysCompRate = calculateLastNDaysCompletionRate(allLogs, habits.size(), 7);
        double maxStreak = habitDtos.stream().mapToInt(HabitDto::getCurrentStreak).max().orElse(0);
        double streakBonus = Math.min(30.0, (maxStreak / 5.0) * 30.0);
        double productivityScore = (last7DaysCompRate * 0.7) + streakBonus;
        productivityScore = Math.min(100.0, Math.round(productivityScore * 10.0) / 10.0);

        // 3. Weekly Progress (Last 7 Days)
        List<AnalyticsDto.WeeklyProgressItem> weeklyProgress = calculateWeeklyProgress(allLogs, habits.size());

        // 4. Monthly Progress (Last 30 Days)
        List<AnalyticsDto.MonthlyProgressItem> monthlyProgress = calculateMonthlyProgress(allLogs);

        // 5. Category Distribution
        List<AnalyticsDto.CategoryDistributionItem> categoryDistribution = calculateCategoryDistribution(habits);

        // 6. Time of Day Analysis
        List<AnalyticsDto.TimeAnalysisItem> timeOfDayAnalysis = calculateTimeOfDayAnalysis(allLogs);

        // 7. Mood Correlation
        List<AnalyticsDto.MoodCorrelationItem> moodCorrelation = calculateMoodCorrelation(allLogs);

        // 8. Weakest Habit calculation
        Optional<HabitDto> weakest = habitDtos.stream()
                .min(Comparator.comparingDouble(HabitDto::getCompletionRate));
        String weakestName = weakest.map(HabitDto::getName).orElse("None");
        double weakestRate = weakest.map(HabitDto::getCompletionRate).orElse(100.0);

        // 9. Failure Recovery Index
        int missedDays = 0;
        int recoveredDays = 0;
        LocalDate today = LocalDate.now();

        for (Habit habit : habits) {
            Set<LocalDate> compDates = allLogs.stream()
                    .filter(l -> l.getHabit().getId().equals(habit.getId()))
                    .map(HabitLog::getCompletedDate)
                    .collect(Collectors.toSet());

            LocalDate start = habit.getCreatedAt().toLocalDate();
            LocalDate curr = start;

            while (curr.isBefore(today)) {
                if (!compDates.contains(curr)) {
                    missedDays++;
                    LocalDate nextDay = curr.plusDays(1);
                    if (compDates.contains(nextDay)) {
                        recoveredDays++;
                    }
                }
                curr = curr.plusDays(1);
            }
        }
        double recoveryIndex = missedDays > 0 ? ((double) recoveredDays / missedDays) * 100.0 : 100.0;
        recoveryIndex = Math.round(recoveryIndex * 10.0) / 10.0;

        return AnalyticsDto.builder()
                .overallConsistencyScore(overallConsistency)
                .productivityScore(productivityScore)
                .weeklyProgress(weeklyProgress)
                .monthlyProgress(monthlyProgress)
                .categoryDistribution(categoryDistribution)
                .timeOfDayAnalysis(timeOfDayAnalysis)
                .moodCorrelation(moodCorrelation)
                .weakestHabitName(weakestName)
                .weakestHabitCompletionRate(weakestRate)
                .failureRecoveryIndex(recoveryIndex)
                .build();
    }

    private double calculateLastNDaysCompletionRate(List<HabitLog> logs, int activeHabitsCount, int days) {
        if (activeHabitsCount == 0 || logs.isEmpty()) return 0.0;
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1);

        long completions = logs.stream()
                .filter(log -> !log.getCompletedDate().isBefore(start) && !log.getCompletedDate().isAfter(today))
                .count();

        double possibleCompletions = activeHabitsCount * days;
        return (completions / possibleCompletions) * 100.0;
    }

    private List<AnalyticsDto.WeeklyProgressItem> calculateWeeklyProgress(List<HabitLog> logs, int activeHabitsCount) {
        List<AnalyticsDto.WeeklyProgressItem> progress = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Generate last 7 days chronologically
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            long completedCount = logs.stream()
                    .filter(log -> log.getCompletedDate().equals(date))
                    .count();

            progress.add(new AnalyticsDto.WeeklyProgressItem(dayName, (int) completedCount, activeHabitsCount));
        }

        return progress;
    }

    private List<AnalyticsDto.MonthlyProgressItem> calculateMonthlyProgress(List<HabitLog> logs) {
        List<AnalyticsDto.MonthlyProgressItem> progress = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Last 15 days is cleaner for rendering on smaller screens, let's generate last 15 days
        for (int i = 14; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String label = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getDayOfMonth();

            long count = logs.stream()
                    .filter(log -> log.getCompletedDate().equals(date))
                    .count();

            progress.add(new AnalyticsDto.MonthlyProgressItem(label, (int) count));
        }

        return progress;
    }

    private List<AnalyticsDto.CategoryDistributionItem> calculateCategoryDistribution(List<Habit> habits) {
        Map<String, Long> grouped = habits.stream()
                .collect(Collectors.groupingBy(Habit::getCategory, Collectors.counting()));

        List<AnalyticsDto.CategoryDistributionItem> dist = new ArrayList<>();
        grouped.forEach((cat, count) -> dist.add(new AnalyticsDto.CategoryDistributionItem(cat, count.intValue())));
        
        return dist;
    }

    private List<AnalyticsDto.TimeAnalysisItem> calculateTimeOfDayAnalysis(List<HabitLog> logs) {
        int morning = 0;
        int afternoon = 0;
        int evening = 0;
        int night = 0;

        for (HabitLog log : logs) {
            LocalTime time = log.getCompletedTime();
            int hour = time.getHour();
            if (hour >= 5 && hour < 12) {
                morning++;
            } else if (hour >= 12 && hour < 17) {
                afternoon++;
            } else if (hour >= 17 && hour < 21) {
                evening++;
            } else {
                night++;
            }
        }

        int total = logs.size();
        List<AnalyticsDto.TimeAnalysisItem> analysis = new ArrayList<>();
        
        analysis.add(new AnalyticsDto.TimeAnalysisItem("Morning (5AM-12PM)", morning, total > 0 ? ((double) morning / total) * 100.0 : 0.0));
        analysis.add(new AnalyticsDto.TimeAnalysisItem("Afternoon (12PM-5PM)", afternoon, total > 0 ? ((double) afternoon / total) * 100.0 : 0.0));
        analysis.add(new AnalyticsDto.TimeAnalysisItem("Evening (5PM-9PM)", evening, total > 0 ? ((double) evening / total) * 100.0 : 0.0));
        analysis.add(new AnalyticsDto.TimeAnalysisItem("Night (9PM-5AM)", night, total > 0 ? ((double) night / total) * 100.0 : 0.0));

        return analysis;
    }

    private List<AnalyticsDto.MoodCorrelationItem> calculateMoodCorrelation(List<HabitLog> logs) {
        Map<String, Integer> moodCounts = new HashMap<>();
        moodCounts.put("HAPPY", 0);
        moodCounts.put("ENERGETIC", 0);
        moodCounts.put("NEUTRAL", 0);
        moodCounts.put("STRESSED", 0);
        moodCounts.put("SAD", 0);

        for (HabitLog log : logs) {
            if (log.getMood() != null) {
                String mood = log.getMood().toUpperCase();
                if (moodCounts.containsKey(mood)) {
                    moodCounts.put(mood, moodCounts.get(mood) + 1);
                }
            }
        }

        List<AnalyticsDto.MoodCorrelationItem> correlation = new ArrayList<>();
        moodCounts.forEach((mood, count) -> correlation.add(new AnalyticsDto.MoodCorrelationItem(mood, count)));
        
        return correlation;
    }
}
