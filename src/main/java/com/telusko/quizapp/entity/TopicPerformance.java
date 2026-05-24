package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the aggregated performance of one user in one topic/category.
 *
 * One row per (user, category) pair. UPSERTED after every quiz submission.
 *
 * Fields used by the scoring algorithm:
 *
 *   totalAttempts      → how many questions attempted in this topic
 *   correctAnswers     → raw correct count
 *   accuracyScore      → correctAnswers / totalAttempts × 100
 *   masteryScore       → weighted score (accuracy × consistency × recency)
 *   avgTimeTaken       → average seconds per question (speed = confidence)
 *   isWeak             → true when masteryScore < weaknessThreshold AND attempts >= 3
 *   lastAttemptedAt    → used for recency weighting and spaced repetition (Phase 4)
 *   trend              → IMPROVING / DECLINING / STABLE — last 3 session comparison
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "topic_performances",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category"}))
public class TopicPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String category;

    @Column(name = "total_attempts")
    private Integer totalAttempts;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    // Raw accuracy: correctAnswers / totalAttempts × 100
    @Column(name = "accuracy_score")
    private Double accuracyScore;

    // Weighted mastery score 0–100 (see WeakTopicDetectionService for formula)
    @Column(name = "mastery_score")
    private Double masteryScore;

    // Average seconds per question — lower = more confident
    @Column(name = "avg_time_taken_seconds")
    private Double avgTimeTakenSeconds;

    // True when mastery < threshold AND enough data exists
    @Column(name = "is_weak")
    private Boolean isWeak;

    // IMPROVING / DECLINING / STABLE
    @Column(name = "trend")
    private String trend;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
