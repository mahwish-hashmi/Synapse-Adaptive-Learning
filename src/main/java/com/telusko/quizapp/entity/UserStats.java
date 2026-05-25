package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks gamification stats for a user.
 *
 * One row per user. Updated after every quiz submission.
 *
 * XP system:
 *   +10 XP  for completing any quiz
 *   +2  XP  for each correct answer
 *   +5  XP  streak bonus (if practiced yesterday too)
 *
 * Level thresholds:
 *   Level 1 → 0–99 XP
 *   Level 2 → 100–249 XP
 *   Level 3 → 250–499 XP
 *   Level 4 → 500–999 XP
 *   Level 5 → 1000+ XP
 *
 * Streak:
 *   currentStreak = consecutive days with at least one quiz completed
 *   If lastQuizDate = yesterday → streak continues
 *   If lastQuizDate < yesterday → streak resets to 0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_stats",
       uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class UserStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_xp")
    @Builder.Default
    private Integer totalXp = 0;

    @Column(name = "current_level")
    @Builder.Default
    private Integer currentLevel = 1;

    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak")
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "total_quizzes_completed")
    @Builder.Default
    private Integer totalQuizzesCompleted = 0;

    @Column(name = "total_questions_answered")
    @Builder.Default
    private Integer totalQuestionsAnswered = 0;

    @Column(name = "total_correct_answers")
    @Builder.Default
    private Integer totalCorrectAnswers = 0;

    // Date of the most recent quiz — used for streak calculation
    @Column(name = "last_quiz_date")
    private LocalDate lastQuizDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
