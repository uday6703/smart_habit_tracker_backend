package com.tracker.smarthabittracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitStackDto {
    private Long id;
    private Long cueHabitId;
    private String cueHabitName;
    private Long targetHabitId;
    private String targetHabitName;
    private LocalDateTime createdAt;
}
