package com.tracker.smarthabittracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mood_logs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "logged_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "logged_date", nullable = false)
    private LocalDate loggedDate;

    @Column(name = "mood_score", nullable = false)
    private Integer moodScore; // 1 to 5

    @Column(name = "stress_level", nullable = false)
    private Integer stressLevel; // 1 to 5

    @Column(name = "energy_level", nullable = false)
    private Integer energyLevel; // 1 to 5

    @Column(name = "motivation_level", nullable = false)
    private Integer motivationLevel; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
