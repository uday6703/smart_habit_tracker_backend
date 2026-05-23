package com.tracker.smarthabittracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodCorrelationResultDto {
    private Double averageMoodWithCompletions;
    private Double averageMoodWithSkips;
    private Double averageStressWithCompletions;
    private Double averageStressWithSkips;
    private List<MoodLogDto> recentLogs;
    private String aiInsightSummary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoodLogDto {
        private String date;
        private Integer moodScore;
        private Integer stressLevel;
        private Integer energyLevel;
        private Integer motivationLevel;
        private String notes;
    }
}
