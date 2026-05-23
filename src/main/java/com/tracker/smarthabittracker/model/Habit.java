package com.tracker.smarthabittracker.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "habits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category; // FITNESS, STUDY, HEALTH, PRODUCTIVITY, MOOD, FINANCE, SOCIAL

    @Column(nullable = false, length = 20)
    private String frequency; // DAILY, WEEKLY, CUSTOM

    @Column(name = "frequency_value", length = 50)
    private String frequencyValue; // e.g. "MON,WED,FRI" or "5"

    @Column(name = "target_count", nullable = false)
    @Builder.Default
    private Integer targetCount = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
