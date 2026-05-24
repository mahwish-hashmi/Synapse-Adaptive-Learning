package com.telusko.quizapp.service;

import com.telusko.quizapp.dto.response.TopicPerformanceResponse;
import com.telusko.quizapp.dto.response.WeaknessReportResponse;
import com.telusko.quizapp.entity.QuestionAttempt;
import com.telusko.quizapp.entity.TopicPerformance;
import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.entity.WeaknessAnalysis;
import com.telusko.quizapp.repository.QuestionAttemptRepository;
import com.telusko.quizapp.repository.QuizAttemptRepository;
import com.telusko.quizapp.repository.TopicPerformanceRepository;
import com.telusko.quizapp.repository.WeaknessAnalysisRepository;
import com.telusko.quizapp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 *  WEAK TOPIC DETECTION AI — Core Scoring Engine
 * ═══════════════════════════════════════════════════════════════
 *
 * This service does three things after every quiz submission:
 *
 *  1. SCORE    — Calculate AccuracyScore, MasteryScore per topic
 *  2. FLAG     — Mark topic as weak if mastery < threshold
 *  3. INSIGHT  — Generate natural language feedback for the student
 *
 * ── SCORING ALGORITHM ──────────────────────────────────────────
 *
 *  AccuracyScore (0–100):
 *    = (correctAnswers / totalAttempts) × 100
 *    Simple ratio. Fast to compute. Used as base input.
 *
 *  MasteryScore (0–100):  ← the main score
 *    = AccuracyScore
 *      × ConsistencyFactor   (rewards steady performance, not flukes)
 *      × VolumeBonus         (rewards more attempts — can't master from 1 question)
 *
 *    ConsistencyFactor = 1 - (stdDev of recent session scores / 100)
 *      High variance → lower mastery (lucky streaks don't count)
 *
 *    VolumeBonus = 1 - (1 / sqrt(attempts))
 *      1 attempt  → bonus = 0.00  (no data yet)
 *      4 attempts → bonus = 0.50
 *      9 attempts → bonus = 0.67
 *      16 attempts→ bonus = 0.75
 *      Asymptotes toward 1.0 — more data = more trustworthy score
 *
 *  WeaknessThreshold:
 *    isWeak = masteryScore < 50 AND totalAttempts >= 3
 *    We require at least 3 attempts before flagging to avoid
 *    false positives from first-attempt failures.
 *
 *  Trend (IMPROVING / DECLINING / STABLE):
 *    Compare average score of last 3 attempts vs previous 3 attempts.
 *    Delta > +10  → IMPROVING
 *    Delta < -10  → DECLINING
 *    Otherwise    → STABLE
 *
 * ── INSIGHT GENERATION ─────────────────────────────────────────
 *
 *  Rule-based engine (Phase 3). Upgraded to LLM calls in Phase 5.
 *  Rules fire in priority order — most severe condition wins.
 *
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeakTopicDetectionService {

    // Minimum attempts before we trust the data enough to flag weakness
    private static final int MIN_ATTEMPTS_FOR_ANALYSIS = 3;

    // Below this mastery score → topic is flagged as weak
    private static final double WEAKNESS_THRESHOLD = 50.0;

    // Above this mastery score → topic is considered strong
    private static final double STRENGTH_THRESHOLD = 75.0;

    private final QuestionAttemptRepository questionAttemptRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final WeaknessAnalysisRepository weaknessAnalysisRepository;
    private final SecurityUtils securityUtils;

    // ─────────────────────────────────────────────────────────────────────────
    // ENTRY POINT — called by QuizService after every quiz submission
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void analyzeAfterQuiz(User user, String category) {
        log.info("Running weakness analysis for userId={}, category={}", user.getId(), category);

        // Step 1: Recalculate TopicPerformance for the submitted category
        recalculateTopicPerformance(user, category);

        // Step 2: Regenerate the full WeaknessAnalysis report across all topics
        regenerateWeaknessReport(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Recalculate TopicPerformance for one category
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void recalculateTopicPerformance(User user, String category) {
        // Load ALL question attempts for this user in this category
        List<QuestionAttempt> attempts = questionAttemptRepository
                .findByUserIdAndCategory(user.getId(), category);

        if (attempts.isEmpty()) return;

        // ── Raw counts ───────────────────────────────────────────────────────
        int total = attempts.size();
        long correct = attempts.stream().filter(QuestionAttempt::getIsCorrect).count();
        double accuracyScore = ((double) correct / total) * 100.0;

        // ── Average response time ─────────────────────────────────────────────
        double avgTime = attempts.stream()
                .mapToLong(a -> a.getTimeTakenSeconds() != null ? a.getTimeTakenSeconds() : 0)
                .average()
                .orElse(0.0);

        // ── Consistency factor ────────────────────────────────────────────────
        // Group attempts by quiz session, get per-session accuracy
        Map<Long, List<QuestionAttempt>> bySession = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getQuizAttempt().getId()));

        List<Double> sessionScores = bySession.values().stream()
                .map(sessionAttempts -> {
                    long c = sessionAttempts.stream().filter(QuestionAttempt::getIsCorrect).count();
                    return (double) c / sessionAttempts.size() * 100.0;
                })
                .collect(Collectors.toList());

        double consistencyFactor = calculateConsistencyFactor(sessionScores);

        // ── Volume bonus ──────────────────────────────────────────────────────
        // Rewards having more data — prevents high mastery from tiny samples
        double volumeBonus = 1.0 - (1.0 / Math.sqrt(total));

        // ── Final mastery score ───────────────────────────────────────────────
        double masteryScore = accuracyScore * consistencyFactor * volumeBonus;
        masteryScore = Math.min(100.0, Math.max(0.0, masteryScore)); // clamp 0–100

        // ── Trend detection ───────────────────────────────────────────────────
        String trend = detectTrend(sessionScores);

        // ── Weakness flag ─────────────────────────────────────────────────────
        boolean isWeak = (masteryScore < WEAKNESS_THRESHOLD) && (total >= MIN_ATTEMPTS_FOR_ANALYSIS);

        // ── Upsert TopicPerformance ───────────────────────────────────────────
        TopicPerformance tp = topicPerformanceRepository
                .findByUserIdAndCategory(user.getId(), category)
                .orElse(TopicPerformance.builder()
                        .user(user)
                        .category(category)
                        .build());

        tp.setTotalAttempts(total);
        tp.setCorrectAnswers((int) correct);
        tp.setAccuracyScore(round(accuracyScore));
        tp.setMasteryScore(round(masteryScore));
        tp.setAvgTimeTakenSeconds(round(avgTime));
        tp.setIsWeak(isWeak);
        tp.setTrend(trend);
        tp.setLastAttemptedAt(LocalDateTime.now());

        topicPerformanceRepository.save(tp);
        log.info("TopicPerformance saved: category={}, mastery={}, isWeak={}",
                category, round(masteryScore), isWeak);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Regenerate full WeaknessAnalysis across all topics
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void regenerateWeaknessReport(User user) {
        List<TopicPerformance> allTopics = topicPerformanceRepository
                .findByUserIdOrderByMasteryScoreAsc(user.getId());

        if (allTopics.isEmpty()) return;

        // ── Overall score — weighted average by attempt count ─────────────────
        double totalWeight = allTopics.stream().mapToInt(TopicPerformance::getTotalAttempts).sum();
        double weightedSum = allTopics.stream()
                .mapToDouble(tp -> tp.getMasteryScore() * tp.getTotalAttempts())
                .sum();
        double overallScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;

        // ── Strongest / weakest (min 3 attempts to qualify) ───────────────────
        String weakestTopic = allTopics.stream()
                .filter(tp -> tp.getTotalAttempts() >= MIN_ATTEMPTS_FOR_ANALYSIS)
                .min(Comparator.comparingDouble(TopicPerformance::getMasteryScore))
                .map(TopicPerformance::getCategory)
                .orElse(null);

        String strongestTopic = allTopics.stream()
                .filter(tp -> tp.getTotalAttempts() >= MIN_ATTEMPTS_FOR_ANALYSIS)
                .max(Comparator.comparingDouble(TopicPerformance::getMasteryScore))
                .map(TopicPerformance::getCategory)
                .orElse(null);

        // ── Consistency score — based on how many topics have been attempted ──
        long totalQuizzes = quizAttemptRepository.countByUserIdAndStatus(
                user.getId(), com.telusko.quizapp.entity.QuizAttempt.Status.COMPLETED);
        double consistencyScore = Math.min(100.0, totalQuizzes * 10.0);

        // ── Generate insight and advice ───────────────────────────────────────
        String primaryInsight = generatePrimaryInsight(allTopics, weakestTopic, overallScore);
        String actionableAdvice = generateActionableAdvice(allTopics, weakestTopic, strongestTopic);

        // ── Upsert WeaknessAnalysis ───────────────────────────────────────────
        WeaknessAnalysis analysis = weaknessAnalysisRepository
                .findByUserId(user.getId())
                .orElse(WeaknessAnalysis.builder().user(user).build());

        analysis.setWeakestTopic(weakestTopic);
        analysis.setStrongestTopic(strongestTopic);
        analysis.setPrimaryInsight(primaryInsight);
        analysis.setActionableAdvice(actionableAdvice);
        analysis.setOverallScore(round(overallScore));
        analysis.setConsistencyScore(round(consistencyScore));
        analysis.setTotalQuizzesTaken((int) totalQuizzes);

        weaknessAnalysisRepository.save(analysis);
        log.info("WeaknessAnalysis updated: userId={}, overallScore={}", user.getId(), round(overallScore));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC — Get full report for current user (called by controller)
    // ─────────────────────────────────────────────────────────────────────────

    public WeaknessReportResponse getReportForCurrentUser() {
        User user = securityUtils.getCurrentUser();
        return buildReport(user);
    }

    public WeaknessReportResponse getReportForUser(Long userId) {
        List<TopicPerformance> allTopics = topicPerformanceRepository
                .findByUserIdOrderByMasteryScoreAsc(userId);

        Optional<WeaknessAnalysis> analysis = weaknessAnalysisRepository.findByUserId(userId);

        return assembleResponse(allTopics, analysis.orElse(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS — Scoring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ConsistencyFactor = 1 - (standardDeviation / 100)
     *
     * If all sessions scored 80% → stdDev = 0 → factor = 1.0 (perfect consistency)
     * If sessions scored 20%, 80%, 50% → stdDev is high → factor drops
     *
     * Range: 0.0 to 1.0
     */
    private double calculateConsistencyFactor(List<Double> sessionScores) {
        if (sessionScores.size() < 2) return 0.8; // default for single session

        double mean = sessionScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = sessionScores.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        return Math.max(0.0, 1.0 - (stdDev / 100.0));
    }

    /**
     * Trend detection — compares last 3 sessions vs previous 3 sessions.
     *
     * Returns: IMPROVING / DECLINING / STABLE
     */
    private String detectTrend(List<Double> sessionScores) {
        if (sessionScores.size() < 2) return "STABLE";

        int size = sessionScores.size();
        if (size < 4) {
            // Only 2–3 sessions — compare first vs last
            double delta = sessionScores.get(size - 1) - sessionScores.get(0);
            if (delta > 10) return "IMPROVING";
            if (delta < -10) return "DECLINING";
            return "STABLE";
        }

        // 4+ sessions: compare avg of last 3 vs avg of previous 3
        double recentAvg = sessionScores.subList(size - 3, size)
                .stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double previousAvg = sessionScores.subList(Math.max(0, size - 6), size - 3)
                .stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double delta = recentAvg - previousAvg;
        if (delta > 10) return "IMPROVING";
        if (delta < -10) return "DECLINING";
        return "STABLE";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS — Insight generation (rule-based, upgraded to LLM Phase 5)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates the main insight message shown on the dashboard.
     * Rules fire in priority order — most critical condition wins.
     */
    private String generatePrimaryInsight(List<TopicPerformance> topics,
                                          String weakestTopic, double overallScore) {
        List<TopicPerformance> weakTopics = topics.stream()
                .filter(tp -> Boolean.TRUE.equals(tp.getIsWeak()))
                .collect(Collectors.toList());

        // Critical weakness — very low mastery
        Optional<TopicPerformance> criticalWeak = topics.stream()
                .filter(tp -> tp.getMasteryScore() != null && tp.getMasteryScore() < 30
                           && tp.getTotalAttempts() >= MIN_ATTEMPTS_FOR_ANALYSIS)
                .findFirst();
        if (criticalWeak.isPresent()) {
            return String.format(
                "You are struggling significantly with %s — scoring only %.0f%% mastery. " +
                "This needs immediate attention before progressing further.",
                criticalWeak.get().getCategory(), criticalWeak.get().getMasteryScore());
        }

        // Multiple weak topics
        if (weakTopics.size() >= 2) {
            String weakNames = weakTopics.stream()
                    .map(TopicPerformance::getCategory)
                    .limit(2)
                    .collect(Collectors.joining(" and "));
            return String.format(
                "You have weak spots in %s. Focused revision in these areas will significantly " +
                "boost your overall score.", weakNames);
        }

        // Single weak topic
        if (weakTopics.size() == 1) {
            TopicPerformance weak = weakTopics.get(0);
            String trendMsg = "IMPROVING".equals(weak.getTrend())
                    ? "You are improving though — keep at it!"
                    : "Focus on this before moving forward.";
            return String.format(
                "%s is your weak area with %.0f%% mastery. %s",
                weak.getCategory(), weak.getMasteryScore(), trendMsg);
        }

        // No weak topics — doing well
        if (overallScore >= 75) {
            return "Excellent performance across all topics! You are ready for advanced challenges.";
        }

        return "You are making good progress. Keep practicing consistently to build mastery.";
    }

    /**
     * Generates the actionable next-step advice.
     */
    private String generateActionableAdvice(List<TopicPerformance> topics,
                                            String weakestTopic, String strongestTopic) {
        // Check for declining trend in any topic
        Optional<TopicPerformance> declining = topics.stream()
                .filter(tp -> "DECLINING".equals(tp.getTrend())
                           && tp.getTotalAttempts() >= MIN_ATTEMPTS_FOR_ANALYSIS)
                .findFirst();
        if (declining.isPresent()) {
            return String.format(
                "Your performance in %s has been declining recently. " +
                "Schedule a revision session for this topic today.",
                declining.get().getCategory());
        }

        // Weak topic exists — recommend revision
        if (weakestTopic != null) {
            Optional<TopicPerformance> weak = topics.stream()
                    .filter(tp -> weakestTopic.equals(tp.getCategory()))
                    .findFirst();
            if (weak.isPresent() && weak.get().getMasteryScore() < 40) {
                return String.format(
                    "Start with Easy difficulty questions in %s. " +
                    "Build fundamentals before attempting Medium or Hard problems.", weakestTopic);
            }
            return String.format(
                "Attempt a focused quiz on %s with Medium difficulty to strengthen this topic.",
                weakestTopic);
        }

        // All topics strong — suggest new topic or harder difficulty
        if (strongestTopic != null) {
            return String.format(
                "You have mastered %s. Try Hard difficulty questions or explore a new topic " +
                "to keep growing.", strongestTopic);
        }

        return "Keep taking quizzes regularly. Consistent practice is the fastest path to mastery.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS — Response assembly
    // ─────────────────────────────────────────────────────────────────────────

    private WeaknessReportResponse buildReport(User user) {
        List<TopicPerformance> allTopics = topicPerformanceRepository
                .findByUserIdOrderByMasteryScoreAsc(user.getId());
        Optional<WeaknessAnalysis> analysis = weaknessAnalysisRepository
                .findByUserId(user.getId());
        return assembleResponse(allTopics, analysis.orElse(null));
    }

    private WeaknessReportResponse assembleResponse(List<TopicPerformance> allTopics,
                                                     WeaknessAnalysis analysis) {
        List<TopicPerformanceResponse> allResponses = allTopics.stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());

        List<TopicPerformanceResponse> weakResponses = allResponses.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsWeak()))
                .collect(Collectors.toList());

        List<TopicPerformanceResponse> strongResponses = allResponses.stream()
                .filter(r -> r.getMasteryScore() != null && r.getMasteryScore() >= STRENGTH_THRESHOLD)
                .collect(Collectors.toList());

        double overallScore = analysis != null && analysis.getOverallScore() != null
                ? analysis.getOverallScore() : 0.0;

        return WeaknessReportResponse.builder()
                .overallScore(overallScore)
                .consistencyScore(analysis != null ? analysis.getConsistencyScore() : 0.0)
                .totalQuizzesTaken(analysis != null ? analysis.getTotalQuizzesTaken() : 0)
                .strongestTopic(analysis != null ? analysis.getStrongestTopic() : null)
                .weakestTopic(analysis != null ? analysis.getWeakestTopic() : null)
                .primaryInsight(analysis != null ? analysis.getPrimaryInsight()
                        : "Take a few quizzes to generate your personalized analysis.")
                .actionableAdvice(analysis != null ? analysis.getActionableAdvice()
                        : "Start with any category to begin tracking your performance.")
                .allTopics(allResponses)
                .weakTopics(weakResponses)
                .strongTopics(strongResponses)
                .generatedAt(analysis != null ? analysis.getGeneratedAt() : null)
                .overallLabel(getMasteryLabel(overallScore))
                .overallMessage(getOverallMessage(overallScore))
                .build();
    }

    private TopicPerformanceResponse toTopicResponse(TopicPerformance tp) {
        return TopicPerformanceResponse.builder()
                .category(tp.getCategory())
                .totalAttempts(tp.getTotalAttempts())
                .correctAnswers(tp.getCorrectAnswers())
                .accuracyScore(tp.getAccuracyScore())
                .masteryScore(tp.getMasteryScore())
                .avgTimeTakenSeconds(tp.getAvgTimeTakenSeconds())
                .isWeak(tp.getIsWeak())
                .trend(tp.getTrend())
                .masteryLabel(getMasteryLabel(tp.getMasteryScore() != null ? tp.getMasteryScore() : 0))
                .lastAttemptedAt(tp.getLastAttemptedAt())
                .build();
    }

    private String getMasteryLabel(double score) {
        if (score >= 85) return "Expert";
        if (score >= 65) return "Proficient";
        if (score >= 40) return "Developing";
        return "Beginner";
    }

    private String getOverallMessage(double score) {
        if (score >= 85) return "Outstanding! You are in the top tier of learners.";
        if (score >= 65) return "Great work! You have a solid understanding across topics.";
        if (score >= 40) return "Good progress. Focus on your weak topics to level up.";
        return "You are just getting started. Consistent practice will show results quickly.";
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
