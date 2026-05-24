package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the AI-generated weakness analysis report for a user.
 *
 * One row per user. Regenerated after every quiz submission.
 *
 * This is what the frontend dashboard displays:
 *   - weakestTopic      → the topic needing most attention right now
 *   - strongestTopic    → the topic the student is best at
 *   - primaryInsight    → main AI message e.g. "You fail Trees 70% of the time"
 *   - actionableAdvice  → what to do next e.g. "Revise recursion before attempting DP"
 *   - overallScore      → aggregate score across all topics (0–100)
 *   - consistencyScore  → how regularly the student practices (0–100)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weakness_analyses",
       uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class WeaknessAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "weakest_topic")
    private String weakestTopic;

    @Column(name = "strongest_topic")
    private String strongestTopic;

    // Main insight message shown on dashboard
    @Column(name = "primary_insight", length = 500)
    private String primaryInsight;

    // Actionable next step for the student
    @Column(name = "actionable_advice", length = 500)
    private String actionableAdvice;

    // Aggregate mastery across all topics (weighted average)
    @Column(name = "overall_score")
    private Double overallScore;

    // How consistently the student is practicing (based on attempt frequency)
    @Column(name = "consistency_score")
    private Double consistencyScore;

    // Total quizzes completed across all topics
    @Column(name = "total_quizzes_taken")
    private Integer totalQuizzesTaken;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        generatedAt = LocalDateTime.now();
    }
}
