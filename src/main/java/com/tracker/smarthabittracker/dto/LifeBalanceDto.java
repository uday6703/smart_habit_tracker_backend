package com.tracker.smarthabittracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifeBalanceDto {
    private Map<String, Double> categoryScores; // health, fitness, learning, productivity, sleep, mentalWellness, discipline, focus
    private String strongestCategory;
    private String weakestCategory;
    private String aiBalanceAnalysis;
}
