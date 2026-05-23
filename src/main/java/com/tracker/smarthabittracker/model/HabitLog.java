package com.tracker.smarthabittracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "habit_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"habit_id", "completed_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "completed_time", nullable = false)
    private LocalTime completedTime;

    @Column(length = 20)
    private String mood; // HAPPY, NEUTRAL, SAD, STRESSED, ENERGETIC

    @Column(columnDefinition = "TEXT")
    private String notes;
}
