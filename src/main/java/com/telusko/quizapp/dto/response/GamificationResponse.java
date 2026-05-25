package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamificationResponse {

    // ── XP and Level ──────────────────────────────────────────────────────────
    private Integer totalXp;
    private Integer currentLevel;
    private String levelName;              // "Beginner" / "Learner" / "Scholar" / "Expert" / "Master"
    private Integer xpForNextLevel;        // XP needed to reach next level
    private Integer xpProgressInLevel;     // XP earned within current level
    private Double levelProgressPercent;   // 0–100% progress bar value

    // ── Streaks ───────────────────────────────────────────────────────────────
    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate lastQuizDate;
    private boolean practicedToday;

    // ── Stats ─────────────────────────────────────────────────────────────────
    private Integer totalQuizzesCompleted;
    private Integer totalQuestionsAnswered;
    private Integer totalCorrectAnswers;
    private Double overallAccuracy;

    // ── Achievements / Badges ─────────────────────────────────────────────────
    private List<BadgeInfo> earnedBadges;
    private List<BadgeInfo> availableBadges;  // Not yet earned
    private Integer totalBadgesEarned;

    // ── Newly earned (returned right after quiz) ──────────────────────────────
    private List<BadgeInfo> newlyEarnedBadges;
    private Integer xpEarnedThisQuiz;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeInfo {
        private String name;
        private String description;
        private String icon;
        private String earnedAt;     // null if not yet earned
        private Integer xpReward;
    }
}
