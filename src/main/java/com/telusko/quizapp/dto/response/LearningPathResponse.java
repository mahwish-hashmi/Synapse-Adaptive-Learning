package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponse {

    // ── What to do next ───────────────────────────────────────────────────────
    private String recommendedNextTopic;
    private String recommendedDifficulty;
    private String learningPhase;          // FOUNDATION / DEVELOPMENT / MASTERY / ADVANCED
    private String studyPlanSummary;

    // ── Ordered topic sequence ────────────────────────────────────────────────
    private List<String> topicSequence;

    // ── Revision alerts ───────────────────────────────────────────────────────
    private List<RevisionScheduleResponse> dueForRevision;  // topics overdue today
    private List<RevisionScheduleResponse> upcomingRevisions; // due in next 7 days

    // ── Adaptive quiz recommendation ──────────────────────────────────────────
    private AdaptiveQuizRecommendation nextQuizRecommendation;

    // ── Progress estimate ─────────────────────────────────────────────────────
    private Integer estimatedCompletionDays;
    private LocalDateTime generatedAt;
}
