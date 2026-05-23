package com.tracker.smarthabittracker.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "habit_stacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cue_habit_id", nullable = false)
    private Habit cueHabit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_habit_id", nullable = false)
    private Habit targetHabit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
