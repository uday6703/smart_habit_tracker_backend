package com.tracker.smarthabittracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnalyticsDto {
    private Double overallConsistencyScore; // percentage
    private Double productivityScore; // weighted score
    private List<WeeklyProgressItem> weeklyProgress;
    private List<MonthlyProgressItem> monthlyProgress;
    private List<CategoryDistributionItem> categoryDistribution;
    private List<TimeAnalysisItem> timeOfDayAnalysis;
    private List<MoodCorrelationItem> moodCorrelation;
    private String weakestHabitName;
    private Double weakestHabitCompletionRate;
    private Double failureRecoveryIndex;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WeeklyProgressItem {
        private String day; // "Mon", "Tue", etc.
        private Integer completed;
        private Integer total;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyProgressItem {
        private String date; // "May 10", "May 11"
        private Integer completedCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryDistributionItem {
        private String category;
        private Integer count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeAnalysisItem {
        private String period; // "Morning", "Afternoon", "Evening", "Night"
        private Integer count;
        private Double percentage;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MoodCorrelationItem {
        private String mood;
        private Integer completedCount;
    }
}
