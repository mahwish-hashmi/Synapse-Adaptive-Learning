package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the AI-generated personalized learning path for a user.
 *
 * One row per user. Regenerated after every quiz as their performance evolves.
 *
 * The learning path answers three questions:
 *   1. What topic should the student focus on NEXT?
 *   2. What difficulty level are they ready for?
 *   3. What is the recommended sequence of topics to reach mastery?
 *
 * How it is generated (LearningPathService):
 *   - Reads all TopicPerformance rows for the user
 *   - Weak topics with low mastery → assigned first, Easy difficulty
 *   - Medium topics (mastery 50–75) → assigned next, Medium difficulty
 *   - Strong topics (mastery > 75) → Hard difficulty or new topics suggested
 *   - Spaced repetition: topics not practiced in X days get scheduled for revision
 *
 * Fields:
 *   recommendedNextTopic    → the single most important topic right now
 *   recommendedDifficulty   → Easy / Medium / Hard
 *   topicSequence           → comma-separated ordered list e.g. "Trees,DP,Graphs"
 *   studyPlanSummary        → human-readable explanation of the plan
 *   estimatedCompletionDays → rough estimate to reach overall mastery >= 75
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "learning_paths",
       uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // The single most important topic for the student right now
    @Column(name = "recommended_next_topic")
    private String recommendedNextTopic;

    // Easy / Medium / Hard — what difficulty is appropriate for next quiz
    @Column(name = "recommended_difficulty")
    private String recommendedDifficulty;

    // Ordered topic sequence — comma separated
    // e.g. "Data Structures,Java,Spring Boot"
    @Column(name = "topic_sequence", length = 1000)
    private String topicSequence;

    // Natural language summary shown on dashboard
    @Column(name = "study_plan_summary", length = 1000)
    private String studyPlanSummary;

    // Rough estimate in days to reach mastery >= 75 across all topics
    @Column(name = "estimated_completion_days")
    private Integer estimatedCompletionDays;

    // Current phase: FOUNDATION / DEVELOPMENT / MASTERY / ADVANCED
    @Column(name = "learning_phase")
    private String learningPhase;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        generatedAt = LocalDateTime.now();
    }
}
