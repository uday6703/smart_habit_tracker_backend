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
public class HeatmapDto {
    private String date;
    private Double completionRate;
    private Integer completedCount;
    private Integer totalCount;
    private List<String> completedHabits;
    private List<String> missedHabits;
    private Double productivityScore;
}
