package com.tracker.smarthabittracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "failure_analysis", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "habit_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "primary_trigger", nullable = false, length = 100)
    private String primaryTrigger; // WEEKEND_DROP, STRESS_CORRELATION, OVERLOAD, etc.

    @Column(name = "ai_explanation", nullable = false, columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "recovery_advice", nullable = false, columnDefinition = "TEXT")
    private String recoveryAdvice;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
