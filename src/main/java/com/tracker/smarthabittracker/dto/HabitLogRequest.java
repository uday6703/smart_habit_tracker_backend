package com.tracker.smarthabittracker.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class HabitLogRequest {
    private LocalDate completedDate;
    private LocalTime completedTime;
    private String mood;
    private String notes;
}
