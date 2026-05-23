package com.tracker.smarthabittracker.dto;

import com.tracker.smarthabittracker.model.HabitLog;
import com.tracker.smarthabittracker.model.Suggestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardSummary {
    private Integer totalHabits;
    private Integer completedTodayCount;
    private Integer bestStreak;
    private Double overallProductivityScore; // Score between 0 and 100
    private List<HabitDto> todaysHabits;
    private List<Suggestion> activeSuggestions;
    private List<RecentActivityDto> recentActivities;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RecentActivityDto {
        private String habitName;
        private String completedDate;
        private String completedTime;
        private String category;
        private String mood;
    }
}
