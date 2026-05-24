package com.telusko.quizapp.service;

import com.telusko.quizapp.dto.response.AdaptiveQuizRecommendation;
import com.telusko.quizapp.dto.response.LearningPathResponse;
import com.telusko.quizapp.dto.response.RevisionScheduleResponse;
import com.telusko.quizapp.entity.LearningPath;
import com.telusko.quizapp.entity.RevisionSchedule;
import com.telusko.quizapp.entity.TopicPerformance;
import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.repository.LearningPathRepository;
import com.telusko.quizapp.repository.RevisionScheduleRepository;
import com.telusko.quizapp.repository.TopicPerformanceRepository;
import com.telusko.quizapp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 *  PERSONALIZED LEARNING PATH AI — Phase 4
 * ═══════════════════════════════════════════════════════════════
 *
 * This service does three things after every quiz:
 *
 *  1. SPACED REPETITION — Update revision schedule per topic
 *  2. LEARNING PATH    — Regenerate ordered topic sequence
 *  3. ADAPTIVE QUIZ    — Recommend exact next quiz parameters
 *
 * ── TOPIC PRIORITIZATION ALGORITHM ────────────────────────────
 *
 *  Topics are ranked for "what to study next" by this priority:
 *
 *   Priority 1 — REVISION DUE
 *     Any topic where nextRevisionDate <= today
 *     Reason: spaced repetition overrides everything
 *
 *   Priority 2 — WEAK + NOT IMPROVING
 *     isWeak = true AND trend != IMPROVING
 *     Reason: needs focused attention before moving on
 *
 *   Priority 3 — WEAK + IMPROVING
 *     isWeak = true AND trend = IMPROVING
 *     Reason: progress is happening, keep momentum
 *
 *   Priority 4 — DEVELOPING (mastery 50–74)
 *     Not weak, but not mastered yet
 *     Reason: consolidate before advancing
 *
 *   Priority 5 — STRONG (mastery >= 75)
 *     Challenge with Hard difficulty or suggest new topics
 *
 * ── DIFFICULTY PROGRESSION LOGIC ──────────────────────────────
 *
 *   masteryScore < 40  → Easy
 *   masteryScore 40–65 → Medium
 *   masteryScore > 65  → Hard
 *
 * ── SPACED REPETITION (SM-2 inspired) ─────────────────────────
 *
 *   Correct session:
 *     interval = previous interval × 2  (capped at 30 days)
 *     consecutiveCorrectSessions++
 *
 *   Incorrect session / weak topic:
 *     interval = 1 day (review tomorrow)
 *     consecutiveCorrectSessions = 0
 *
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathService {

    private static final int MAX_INTERVAL_DAYS = 30;
    private static final int DEFAULT_START_INTERVAL = 1;

    private final TopicPerformanceRepository topicPerformanceRepository;
    private final LearningPathRepository learningPathRepository;
    private final RevisionScheduleRepository revisionScheduleRepository;
    private final SecurityUtils securityUtils;

    // ─────────────────────────────────────────────────────────────────────────
    // ENTRY POINT — called by QuizService after every quiz submission
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void updateAfterQuiz(User user, String category, double sessionScore) {
        log.info("Updating learning path for userId={}, category={}, score={}",
                user.getId(), category, sessionScore);

        // Step 1: Update spaced repetition schedule for this category
        updateRevisionSchedule(user, category, sessionScore);

        // Step 2: Regenerate full learning path across all topics
        regenerateLearningPath(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Spaced Repetition Schedule Update
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void updateRevisionSchedule(User user, String category, double sessionScore) {
        RevisionSchedule schedule = revisionScheduleRepository
                .findByUserIdAndCategory(user.getId(), category)
                .orElse(RevisionSchedule.builder()
                        .user(user)
                        .category(category)
                        .intervalDays(DEFAULT_START_INTERVAL)
                        .consecutiveCorrectSessions(0)
                        .build());

        boolean sessionPassed = sessionScore >= 60.0;

        if (sessionPassed) {
            // Good session → increase interval (space out next revision)
            int newInterval = Math.min(
                    (schedule.getIntervalDays() != null ? schedule.getIntervalDays() : 1) * 2,
                    MAX_INTERVAL_DAYS
            );
            int consecutive = (schedule.getConsecutiveCorrectSessions() != null
                    ? schedule.getConsecutiveCorrectSessions() : 0) + 1;

            schedule.setIntervalDays(newInterval);
            schedule.setConsecutiveCorrectSessions(consecutive);
            schedule.setNextRevisionDate(LocalDate.now().plusDays(newInterval));
            schedule.setPriority(consecutive >= 3 ? "LOW" : "MEDIUM");
        } else {
            // Bad session → reset to 1 day (review tomorrow)
            schedule.setIntervalDays(1);
            schedule.setConsecutiveCorrectSessions(0);
            schedule.setNextRevisionDate(LocalDate.now().plusDays(1));
            schedule.setPriority("HIGH");
        }

        schedule.setLastPracticedDate(LocalDate.now());
        schedule.setIsDueToday(false); // just practiced today

        revisionScheduleRepository.save(schedule);
        log.info("RevisionSchedule updated: category={}, nextRevision={}, interval={}d",
                category, schedule.getNextRevisionDate(), schedule.getIntervalDays());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Regenerate Learning Path
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void regenerateLearningPath(User user) {
        List<TopicPerformance> allTopics = topicPerformanceRepository
                .findByUserIdOrderByMasteryScoreAsc(user.getId());

        if (allTopics.isEmpty()) return;

        // Get overdue revisions — they always take priority
        List<RevisionSchedule> dueRevisions = revisionScheduleRepository
                .findDueForRevision(user.getId(), LocalDate.now());

        // Mark due topics
        dueRevisions.forEach(rs -> {
            rs.setIsDueToday(true);
            revisionScheduleRepository.save(rs);
        });

        // Build ordered topic sequence
        List<String> topicSequence = buildTopicSequence(allTopics, dueRevisions);

        // Determine recommended next topic + difficulty
        String nextTopic = topicSequence.isEmpty() ? null : topicSequence.get(0);
        String nextDifficulty = determineRecommendedDifficulty(nextTopic, allTopics);

        // Determine learning phase
        String learningPhase = determineLearningPhase(allTopics);

        // Generate study plan summary
        String summary = generateStudyPlanSummary(allTopics, nextTopic, dueRevisions.size());

        // Estimate days to completion
        int estimatedDays = estimateCompletionDays(allTopics);

        // Upsert LearningPath
        LearningPath path = learningPathRepository
                .findByUserId(user.getId())
                .orElse(LearningPath.builder().user(user).build());

        path.setRecommendedNextTopic(nextTopic);
        path.setRecommendedDifficulty(nextDifficulty);
        path.setTopicSequence(String.join(",", topicSequence));
        path.setStudyPlanSummary(summary);
        path.setEstimatedCompletionDays(estimatedDays);
        path.setLearningPhase(learningPhase);

        learningPathRepository.save(path);
        log.info("LearningPath updated: userId={}, nextTopic={}, phase={}",
                user.getId(), nextTopic, learningPhase);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC — Get learning path for current user
    // ─────────────────────────────────────────────────────────────────────────

    public LearningPathResponse getLearningPathForCurrentUser() {
        User user = securityUtils.getCurrentUser();
        return buildResponse(user);
    }

    public LearningPathResponse getLearningPathForUser(Long userId) {
        Optional<LearningPath> path = learningPathRepository.findByUserId(userId);
        List<RevisionSchedule> due = revisionScheduleRepository
                .findDueForRevision(userId, LocalDate.now());
        List<RevisionSchedule> upcoming = revisionScheduleRepository
                .findUpcomingRevisions(userId, LocalDate.now(), LocalDate.now().plusDays(7));
        List<TopicPerformance> topics = topicPerformanceRepository
                .findByUserIdOrderByMasteryScoreAsc(userId);

        return assembleResponse(path.orElse(null), due, upcoming, topics);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS — Topic prioritization
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the ordered topic sequence based on priority rules.
     *
     * Order:
     *  1. Revision overdue topics (spaced repetition)
     *  2. Weak + not improving
     *  3. Weak + improving
     *  4. Developing (50–74 mastery)
     *  5. Strong topics (challenge mode)
     */
    private List<String> buildTopicSequence(List<TopicPerformance> topics,
                                             List<RevisionSchedule> dueRevisions) {
        Set<String> dueCategories = dueRevisions.stream()
                .map(RevisionSchedule::getCategory)
                .collect(Collectors.toSet());

        List<String> sequence = new ArrayList<>();

        // Priority 1: overdue revisions
        dueRevisions.stream()
                .sorted(Comparator.comparing(RevisionSchedule::getNextRevisionDate))
                .map(RevisionSchedule::getCategory)
                .forEach(sequence::add);

        // Priority 2: weak + not improving
        topics.stream()
                .filter(tp -> Boolean.TRUE.equals(tp.getIsWeak())
                        && !"IMPROVING".equals(tp.getTrend())
                        && !dueCategories.contains(tp.getCategory()))
                .map(TopicPerformance::getCategory)
                .forEach(sequence::add);

        // Priority 3: weak + improving
        topics.stream()
                .filter(tp -> Boolean.TRUE.equals(tp.getIsWeak())
                        && "IMPROVING".equals(tp.getTrend())
                        && !sequence.contains(tp.getCategory()))
                .map(TopicPerformance::getCategory)
                .forEach(sequence::add);

        // Priority 4: developing (mastery 50–74)
        topics.stream()
                .filter(tp -> tp.getMasteryScore() != null
                        && tp.getMasteryScore() >= 50
                        && tp.getMasteryScore() < 75
                        && !sequence.contains(tp.getCategory()))
                .sorted(Comparator.comparingDouble(TopicPerformance::getMasteryScore))
                .map(TopicPerformance::getCategory)
                .forEach(sequence::add);

        // Priority 5: strong topics (mastery >= 75)
        topics.stream()
                .filter(tp -> tp.getMasteryScore() != null
                        && tp.getMasteryScore() >= 75
                        && !sequence.contains(tp.getCategory()))
                .sorted(Comparator.comparingDouble(TopicPerformance::getMasteryScore).reversed())
                .map(TopicPerformance::getCategory)
                .forEach(sequence::add);

        return sequence;
    }

    /**
     * Determines recommended difficulty for a topic based on mastery score.
     *
     *  mastery < 40  → Easy   (build fundamentals)
     *  mastery 40–65 → Medium (consolidate understanding)
     *  mastery > 65  → Hard   (push toward mastery)
     */
    private String determineRecommendedDifficulty(String topic, List<TopicPerformance> topics) {
        if (topic == null) return "Easy";

        return topics.stream()
                .filter(tp -> topic.equals(tp.getCategory()))
                .findFirst()
                .map(tp -> {
                    double mastery = tp.getMasteryScore() != null ? tp.getMasteryScore() : 0;
                    if (mastery < 40) return "Easy";
                    if (mastery < 65) return "Medium";
                    return "Hard";
                })
                .orElse("Easy");
    }

    /**
     * Learning phase based on overall average mastery.
     *
     *  avg < 30  → FOUNDATION  (just getting started)
     *  avg 30–55 → DEVELOPMENT (building skills)
     *  avg 55–75 → MASTERY     (getting proficient)
     *  avg > 75  → ADVANCED    (ready for challenges)
     */
    private String determineLearningPhase(List<TopicPerformance> topics) {
        if (topics.isEmpty()) return "FOUNDATION";

        double avg = topics.stream()
                .mapToDouble(tp -> tp.getMasteryScore() != null ? tp.getMasteryScore() : 0)
                .average()
                .orElse(0);

        if (avg < 30) return "FOUNDATION";
        if (avg < 55) return "DEVELOPMENT";
        if (avg < 75) return "MASTERY";
        return "ADVANCED";
    }

    /**
     * Rough estimate: each topic needs approx (75 - currentMastery) / 10 days
     * to reach mastery target of 75, assuming one quiz per day.
     */
    private int estimateCompletionDays(List<TopicPerformance> topics) {
        return topics.stream()
                .filter(tp -> tp.getMasteryScore() == null || tp.getMasteryScore() < 75)
                .mapToInt(tp -> {
                    double gap = 75 - (tp.getMasteryScore() != null ? tp.getMasteryScore() : 0);
                    return (int) Math.ceil(gap / 10.0);
                })
                .sum();
    }

    private String generateStudyPlanSummary(List<TopicPerformance> topics,
                                             String nextTopic, int overdueCount) {
        long weakCount = topics.stream().filter(tp -> Boolean.TRUE.equals(tp.getIsWeak())).count();
        long strongCount = topics.stream()
                .filter(tp -> tp.getMasteryScore() != null && tp.getMasteryScore() >= 75).count();

        if (overdueCount > 0) {
            return String.format(
                "You have %d topic(s) overdue for revision. Start with %s today. " +
                "Consistent revision is the fastest path to mastery.",
                overdueCount, nextTopic != null ? nextTopic : "your weakest topic");
        }
        if (weakCount > 0) {
            return String.format(
                "Focus on your %d weak topic(s) — especially %s. " +
                "Once you reach 50%% mastery, you will unlock the next difficulty level.",
                weakCount, nextTopic != null ? nextTopic : "identified weak areas");
        }
        if (strongCount == topics.size()) {
            return "You have mastered all studied topics. " +
                   "Try Hard difficulty or explore a new topic to keep growing.";
        }
        return String.format(
            "You are making solid progress. Continue with %s to build consistent mastery " +
            "across all topics.", nextTopic != null ? nextTopic : "your current topics");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS — Response assembly
    // ─────────────────────────────────────────────────────────────────────────

    private LearningPathResponse buildResponse(User user) {
        Optional<LearningPath> path = learningPathRepository.findByUserId(user.getId());
        List<RevisionSchedule> due = revisionScheduleRepository
                .findDueForRevision(user.getId(), LocalDate.now());
        List<RevisionSchedule> upcoming = revisionScheduleRepository
                .findUpcomingRevisions(user.getId(), LocalDate.now(), LocalDate.now().plusDays(7));
        List<TopicPerformance> topics = topicPerformanceRepository
                .findByUserIdOrderByMasteryScoreAsc(user.getId());

        return assembleResponse(path.orElse(null), due, upcoming, topics);
    }

    private LearningPathResponse assembleResponse(LearningPath path,
                                                   List<RevisionSchedule> due,
                                                   List<RevisionSchedule> upcoming,
                                                   List<TopicPerformance> topics) {
        String nextTopic = path != null ? path.getRecommendedNextTopic() : null;
        String nextDifficulty = path != null ? path.getRecommendedDifficulty() : "Easy";

        List<String> topicSequence = path != null && path.getTopicSequence() != null
                ? Arrays.asList(path.getTopicSequence().split(","))
                : Collections.emptyList();

        AdaptiveQuizRecommendation nextQuiz = buildAdaptiveRecommendation(
                nextTopic, nextDifficulty, due, topics);

        return LearningPathResponse.builder()
                .recommendedNextTopic(nextTopic)
                .recommendedDifficulty(nextDifficulty)
                .learningPhase(path != null ? path.getLearningPhase() : "FOUNDATION")
                .studyPlanSummary(path != null ? path.getStudyPlanSummary()
                        : "Take a few quizzes to generate your personalized learning path.")
                .topicSequence(topicSequence)
                .dueForRevision(toRevisionResponses(due))
                .upcomingRevisions(toRevisionResponses(upcoming))
                .nextQuizRecommendation(nextQuiz)
                .estimatedCompletionDays(path != null ? path.getEstimatedCompletionDays() : null)
                .generatedAt(path != null ? path.getGeneratedAt() : null)
                .build();
    }

    /**
     * Builds the adaptive quiz recommendation — tells the frontend
     * exactly what quiz to auto-start next.
     */
    private AdaptiveQuizRecommendation buildAdaptiveRecommendation(
            String nextTopic, String nextDifficulty,
            List<RevisionSchedule> due, List<TopicPerformance> topics) {

        if (nextTopic == null) {
            return AdaptiveQuizRecommendation.builder()
                    .category("Java")
                    .difficulty("Easy")
                    .questionCount(10)
                    .quizType("NEW_TOPIC")
                    .reason("Start your first quiz to begin building your personalized learning path.")
                    .build();
        }

        // Overdue revision takes highest priority
        if (!due.isEmpty() && due.stream().anyMatch(r -> nextTopic.equals(r.getCategory()))) {
            return AdaptiveQuizRecommendation.builder()
                    .category(nextTopic)
                    .difficulty(nextDifficulty)
                    .questionCount(10)
                    .quizType("REVISION")
                    .reason(String.format(
                        "%s is overdue for revision. Practice it now to keep your mastery from fading.",
                        nextTopic))
                    .build();
        }

        // Check if topic is weak
        boolean isWeak = topics.stream()
                .anyMatch(tp -> nextTopic.equals(tp.getCategory())
                        && Boolean.TRUE.equals(tp.getIsWeak()));

        if (isWeak) {
            return AdaptiveQuizRecommendation.builder()
                    .category(nextTopic)
                    .difficulty(nextDifficulty)
                    .questionCount(10)
                    .quizType("PROGRESSION")
                    .reason(String.format(
                        "%s needs focused practice. Starting with %s difficulty to build your foundation.",
                        nextTopic, nextDifficulty))
                    .build();
        }

        // Strong topic — challenge mode
        return AdaptiveQuizRecommendation.builder()
                .category(nextTopic)
                .difficulty(nextDifficulty)
                .questionCount(10)
                .quizType("CHALLENGE")
                .reason(String.format(
                    "You are performing well. Take a %s difficulty %s quiz to push toward mastery.",
                    nextDifficulty, nextTopic))
                .build();
    }

    private List<RevisionScheduleResponse> toRevisionResponses(List<RevisionSchedule> schedules) {
        return schedules.stream()
                .map(rs -> RevisionScheduleResponse.builder()
                        .category(rs.getCategory())
                        .nextRevisionDate(rs.getNextRevisionDate())
                        .lastPracticedDate(rs.getLastPracticedDate())
                        .intervalDays(rs.getIntervalDays())
                        .consecutiveCorrectSessions(rs.getConsecutiveCorrectSessions())
                        .isDueToday(rs.getIsDueToday())
                        .priority(rs.getPriority())
                        .daysUntilDue(rs.getNextRevisionDate() != null
                                ? ChronoUnit.DAYS.between(LocalDate.now(), rs.getNextRevisionDate())
                                : 0)
                        .build())
                .collect(Collectors.toList());
    }
}
