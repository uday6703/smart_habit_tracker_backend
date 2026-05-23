package com.tracker.smarthabittracker.service;

import com.tracker.smarthabittracker.dto.DashboardSummary;
import com.tracker.smarthabittracker.dto.HabitDto;
import com.tracker.smarthabittracker.model.*;
import com.tracker.smarthabittracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SuggestionRepository suggestionRepository;
    private final HabitService habitService;
    private final AnalyticsService analyticsService;

    public DashboardService(HabitRepository habitRepository,
                            HabitLogRepository habitLogRepository,
                            SuggestionRepository suggestionRepository,
                            HabitService habitService,
                            AnalyticsService analyticsService) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.suggestionRepository = suggestionRepository;
        this.habitService = habitService;
        this.analyticsService = analyticsService;
    }

    public DashboardSummary getDashboardSummary(User user) {
        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<HabitDto> habitDtos = habits.stream()
                .map(habitService::mapToDto)
                .toList();

        // 1. Total Habits count
        int totalHabits = habitDtos.size();

        // 2. Count Completed Today
        int completedTodayCount = (int) habitDtos.stream()
                .filter(HabitDto::getCompletedToday)
                .count();

        // 3. Best Current Streak
        int bestStreak = habitDtos.stream()
                .mapToInt(HabitDto::getCurrentStreak)
                .max()
                .orElse(0);

        // 4. Productivity Score (derived from AnalyticsService)
        double prodScore = analyticsService.getAnalyticsForUser(user).getProductivityScore();

        // 5. Today's Scheduled Habits
        List<HabitDto> todaysHabits = filterHabitsForToday(habitDtos);

        // 6. Active Suggestions (Unread suggestions)
        List<Suggestion> activeSuggestions = suggestionRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());

        // 7. Recent Completion Activities (Last 5 logs)
        List<HabitLog> allLogs = habitLogRepository.findByHabitUserId(user.getId());
        List<DashboardSummary.RecentActivityDto> recentActivities = allLogs.stream()
                .sorted((a, b) -> {
                    int dateCompare = b.getCompletedDate().compareTo(a.getCompletedDate());
                    if (dateCompare != 0) return dateCompare;
                    return b.getCompletedTime().compareTo(a.getCompletedTime());
                })
                .limit(6)
                .map(log -> DashboardSummary.RecentActivityDto.builder()
                        .habitName(log.getHabit().getName())
                        .completedDate(log.getCompletedDate().toString())
                        .completedTime(log.getCompletedTime().toString().substring(0, 5))
                        .category(log.getHabit().getCategory())
                        .mood(log.getMood())
                        .build())
                .collect(Collectors.toList());

        return DashboardSummary.builder()
                .totalHabits(totalHabits)
                .completedTodayCount(completedTodayCount)
                .bestStreak(bestStreak)
                .overallProductivityScore(prodScore)
                .todaysHabits(todaysHabits)
                .activeSuggestions(activeSuggestions)
                .recentActivities(recentActivities)
                .build();
    }

    private List<HabitDto> filterHabitsForToday(List<HabitDto> habitDtos) {
        LocalDate today = LocalDate.now();
        String dayOfWeekAbbr = today.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(); // e.g. "MON", "TUE"

        return habitDtos.stream()
                .filter(h -> {
                    if (h.getFrequency().equalsIgnoreCase("DAILY") || h.getFrequency().equalsIgnoreCase("WEEKLY")) {
                        return true;
                    } else if (h.getFrequency().equalsIgnoreCase("CUSTOM")) {
                        if (h.getFrequencyValue() == null) return true;
                        String val = h.getFrequencyValue().toUpperCase();
                        return val.contains(dayOfWeekAbbr);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
}
