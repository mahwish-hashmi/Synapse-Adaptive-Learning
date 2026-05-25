package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Defines a badge/achievement that users can earn.
 *
 * Seeded by DataSeeder at startup — these are the definitions.
 * UserAchievement tracks which users have earned which achievement.
 *
 * conditionType + conditionValue determine when it's awarded:
 *
 *   QUIZ_COUNT      → earned when totalQuizzesCompleted >= conditionValue
 *   STREAK_DAYS     → earned when currentStreak >= conditionValue
 *   XP_THRESHOLD    → earned when totalXp >= conditionValue
 *   PERFECT_SCORE   → earned when any quiz scores 100%
 *   MASTERY_TOPIC   → earned when any topic reaches masteryScore >= conditionValue
 *   FIRST_QUIZ      → earned on first quiz completion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    // Emoji or icon identifier shown in UI
    private String icon;

    // What triggers this achievement
    @Column(name = "condition_type", nullable = false)
    private String conditionType;

    // The threshold value for the condition
    @Column(name = "condition_value")
    private Integer conditionValue;

    // XP reward for earning this badge
    @Column(name = "xp_reward")
    @Builder.Default
    private Integer xpReward = 0;
}
