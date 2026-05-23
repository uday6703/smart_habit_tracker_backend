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
public class HabitDnaProfileDto {
    private String personalityType;
    private List<String> strengths;
    private List<String> weaknesses;
    private String aiCoachingAdvice;
    private String lastUpdated;
}
