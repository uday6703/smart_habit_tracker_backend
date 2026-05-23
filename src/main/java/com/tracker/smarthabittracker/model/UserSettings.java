package com.tracker.smarthabittracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "user_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "daily_reminder_time")
    @Builder.Default
    private LocalTime dailyReminderTime = LocalTime.of(20, 0, 0); // 8:00 PM

    @Column(name = "enable_browser_notifications", nullable = false)
    @Builder.Default
    private Boolean enableBrowserNotifications = true;

    @Column(length = 10, nullable = false)
    @Builder.Default
    private String theme = "DARK";
}
