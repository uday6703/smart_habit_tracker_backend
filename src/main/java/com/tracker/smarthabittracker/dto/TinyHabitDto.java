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
public class TinyHabitDto {
    private Long id;
    private Long originalHabitId;
    private String originalHabitName;
    private Integer originalTargetCount;
    private String suggestedName;
    private Integer suggestedTargetCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime adoptedAt;
}
