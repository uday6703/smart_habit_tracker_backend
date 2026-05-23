package com.tracker.smarthabittracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HabitDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String frequency;
    private String frequencyValue;
    private Integer targetCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    // Aggregated Metrics
    private Integer currentStreak;
    private Integer longestStreak;
    private Double completionRate;
    private Boolean completedToday;
}
