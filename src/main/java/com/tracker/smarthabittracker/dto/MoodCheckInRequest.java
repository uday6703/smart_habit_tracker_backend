package com.tracker.smarthabittracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoodCheckInRequest {

    @NotNull(message = "Mood score is required")
    @Min(1)
    @Max(5)
    private Integer moodScore;

    @NotNull(message = "Stress level is required")
    @Min(1)
    @Max(5)
    private Integer stressLevel;

    @NotNull(message = "Energy level is required")
    @Min(1)
    @Max(5)
    private Integer energyLevel;

    @NotNull(message = "Motivation level is required")
    @Min(1)
    @Max(5)
    private Integer motivationLevel;

    private String notes;
}
