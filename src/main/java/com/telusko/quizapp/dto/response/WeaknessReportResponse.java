package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The complete analytics report shown on the student dashboard.
 *
 * Contains:
 *  - Overall scores and labels
 *  - Per-topic breakdown (one entry per category attempted)
 *  - AI-generated insight and advice
 *  - Weak topics list and strong topics list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeaknessReportResponse {

    // ── Summary ───────────────────────────────────────────────────────────────
    private Double overallScore;         // 0–100 weighted average across topics
    private Double consistencyScore;     // 0–100 practice regularity score
    private Integer totalQuizzesTaken;
    private String strongestTopic;
    private String weakestTopic;

    // ── AI Insights ───────────────────────────────────────────────────────────
    private String primaryInsight;       // "You fail Tree questions 70% of the time"
    private String actionableAdvice;     // "Revise recursion before attempting DP"

    // ── Per-topic breakdown ───────────────────────────────────────────────────
    private List<TopicPerformanceResponse> allTopics;   // sorted by masteryScore asc
    private List<TopicPerformanceResponse> weakTopics;  // isWeak = true only
    private List<TopicPerformanceResponse> strongTopics;// mastery >= 75

    private LocalDateTime generatedAt;

    // ── Helper labels ─────────────────────────────────────────────────────────
    private String overallLabel;   // "Beginner" / "Developing" / "Proficient" / "Expert"
    private String overallMessage; // Encouraging message based on overall score
}
