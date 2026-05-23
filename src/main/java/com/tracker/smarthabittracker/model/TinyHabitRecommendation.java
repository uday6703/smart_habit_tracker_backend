package com.tracker.smarthabittracker.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tiny_habit_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinyHabitRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_habit_id", nullable = false)
    private Habit originalHabit;

    @Column(name = "suggested_name", nullable = false, length = 100)
    private String suggestedName;

    @Column(name = "suggested_target_count", nullable = false)
    private Integer suggestedTargetCount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, DISMISSED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "adopted_at")
    private LocalDateTime adoptedAt;
}
