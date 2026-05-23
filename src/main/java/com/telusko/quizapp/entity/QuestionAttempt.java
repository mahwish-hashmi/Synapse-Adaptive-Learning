package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * The most important table in the platform.
 *
 * One row = one answer given by one user to one question.
 *
 * Why every field matters for Phase 3 AI:
 *
 *   isCorrect           → accuracy score per topic
 *   timeTakenSeconds    → confidence signal (slow = uncertain)
 *   selectedAnswer      → pattern analysis (which wrong answer chosen)
 *   category            → denormalized for fast analytics queries
 *   difficultyLevel     → difficulty progression tracking
 *   attemptNumber       → tracks improvement over multiple attempts
 *
 * This table will have the most rows — index carefully.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "question_attempts", indexes = {
    // These indexes make Phase 3 analytics queries fast
    @Index(name = "idx_qa_user_id",     columnList = "user_id"),
    @Index(name = "idx_qa_category",    columnList = "category"),
    @Index(name = "idx_qa_user_cat",    columnList = "user_id, category")
})
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // What the user actually chose
    @Column(name = "selected_answer")
    private String selectedAnswer;

    // Correct answer duplicated here for fast reads (no join needed)
    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    // Denormalized from Question — avoids joins in analytics queries
    private String category;

    @Column(name = "difficulty_level")
    private String difficultyLevel;

    // Seconds spent on this specific question
    @Column(name = "time_taken_seconds")
    private Long timeTakenSeconds;

    // How many times this user has attempted this question total
    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = LocalDateTime.now();
    }
}
