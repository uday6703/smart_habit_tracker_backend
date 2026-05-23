package com.tracker.smarthabittracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSettingsDto {
    private String dailyReminderTime; // Format: "HH:mm"
    private Boolean enableBrowserNotifications;
    private String theme;
}
