package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Implements spaced repetition scheduling.
 *
 * One row per (user, category) pair.
 *
 * Spaced repetition algorithm (SM-2 inspired):
 *
 *   When a topic is answered CORRECTLY:
 *     interval doubles → nextRevisionDate moves further out
 *     1 day → 3 days → 7 days → 14 days → 30 days
 *
 *   When a topic is answered INCORRECTLY (or isWeak = true):
 *     interval resets → nextRevisionDate = tomorrow
 *     Forces the student back to review before moving on
 *
 *   isDueToday = nextRevisionDate <= today
 *     → Used by LearningPathService to prioritize overdue topics
 *
 * This is what separates good learning platforms from basic quizzes.
 * It ensures weak topics are reviewed frequently, strong topics less often.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "revision_schedules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category"}))
public class RevisionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String category;

    // Current interval in days before next revision is needed
    @Column(name = "interval_days")
    private Integer intervalDays;

    // The date the student should next practice this topic
    @Column(name = "next_revision_date")
    private LocalDate nextRevisionDate;

    // Last time the student practiced this topic
    @Column(name = "last_practiced_date")
    private LocalDate lastPracticedDate;

    // How many consecutive correct sessions in a row
    @Column(name = "consecutive_correct_sessions")
    private Integer consecutiveCorrectSessions;

    // Whether this topic is currently overdue for revision
    @Column(name = "is_due_today")
    private Boolean isDueToday;

    // Priority: HIGH / MEDIUM / LOW
    @Column(name = "priority")
    private String priority;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
