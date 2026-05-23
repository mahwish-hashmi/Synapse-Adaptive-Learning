package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one complete quiz session taken by a user.
 *
 * Lifecycle:
 *   IN_PROGRESS  → user started, not yet submitted
 *   COMPLETED    → user submitted answers, score calculated
 *   ABANDONED    → user never submitted (future: auto-expire)
 *
 * This table feeds Phase 3 analytics:
 *   - totalQuestions, correctAnswers → accuracy per session
 *   - category                       → per-topic performance
 *   - timeTakenSeconds               → speed/confidence signal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which user took this quiz
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Topic/category of this quiz e.g. "Java", "Data Structures"
    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer incorrectAnswers;

    // Score as a percentage 0–100
    @Column(name = "score_percentage")
    private Double scorePercentage;

    // Total seconds from start to submission
    @Column(name = "time_taken_seconds")
    private Long timeTakenSeconds;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // All question-level results for this attempt
    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuestionAttempt> questionAttempts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    public enum Status {
        IN_PROGRESS,
        COMPLETED,
        ABANDONED
    }
}
