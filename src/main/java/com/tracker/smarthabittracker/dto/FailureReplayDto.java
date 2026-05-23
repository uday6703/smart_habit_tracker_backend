package com.tracker.smarthabittracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureReplayDto {
    private Long habitId;
    private String habitName;
    private String category;
    private String primaryTrigger;
    private String aiExplanation;
    private String recoveryAdvice;
}
