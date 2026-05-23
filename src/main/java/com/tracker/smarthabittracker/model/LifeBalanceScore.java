package com.tracker.smarthabittracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "life_balance_scores", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "recorded_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifeBalanceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "health_score", nullable = false)
    private Double healthScore;

    @Column(name = "fitness_score", nullable = false)
    private Double fitnessScore;

    @Column(name = "learning_score", nullable = false)
    private Double learningScore;

    @Column(name = "productivity_score", nullable = false)
    private Double productivityScore;

    @Column(name = "sleep_score", nullable = false)
    private Double sleepScore;

    @Column(name = "mental_wellness_score", nullable = false)
    private Double mentalWellnessScore;

    @Column(name = "discipline_score", nullable = false)
    private Double disciplineScore;

    @Column(name = "focus_score", nullable = false)
    private Double focusScore;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;
}
