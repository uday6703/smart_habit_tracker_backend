package com.tracker.smarthabittracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "future_projections", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "time_horizon_days"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FutureProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "time_horizon_days", nullable = false)
    private Integer timeHorizonDays; // 30, 90, 180

    @Column(name = "projected_productivity_score", nullable = false)
    private Double projectedProductivityScore;

    @Column(name = "narrative_summary", nullable = false, columnDefinition = "TEXT")
    private String narrativeSummary;

    @Column(name = "growth_explanations", nullable = false, columnDefinition = "TEXT")
    private String growthExplanations;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
