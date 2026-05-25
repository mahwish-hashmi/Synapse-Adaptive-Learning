package com.telusko.quizapp.service;

import com.telusko.quizapp.dto.response.GamificationResponse;
import com.telusko.quizapp.dto.response.LeaderboardResponse;
import com.telusko.quizapp.entity.Achievement;
import com.telusko.quizapp.entity.TopicPerformance;
import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.entity.UserAchievement;
import com.telusko.quizapp.entity.UserStats;
import com.telusko.quizapp.repository.AchievementRepository;
import com.telusko.quizapp.repository.TopicPerformanceRepository;
import com.telusko.quizapp.repository.UserAchievementRepository;
import com.telusko.quizapp.repository.UserStatsRepository;
import com.telusko.quizapp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 *  GAMIFICATION SERVICE — Phase 5
 * ═══════════════════════════════════════════════════════════════
 *
 * Handles XP, levels, streaks, badges, and leaderboard.
 * Called from QuizService after every submission.
 *
 * XP CALCULATION per quiz:
 *   base       = 10 XP (just for completing)
 *   correct    = 2 XP per correct answer
 *   streak     = 5 XP bonus if practiced yesterday
 *   Total max for 10-question quiz = 10 + 20 + 5 = 35 XP
 *
 * LEVEL THRESHOLDS:
 *   Level 1 Beginner  →   0–99 XP
 *   Level 2 Learner   → 100–249 XP
 *   Level 3 Scholar   → 250–499 XP
 *   Level 4 Expert    → 500–999 XP
 *   Level 5 Master    → 1000+ XP
 *
 * ACHIEVEMENT EVALUATION:
 *   After every quiz, check ALL achievement conditions.
 *   Any newly satisfied conditions → award badge + bonus XP.
 *   Never award the same badge twice.
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserStatsRepository userStatsRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final TopicPerformanceRepository topicPerformanceRepository;
    private final SecurityUtils securityUtils;

    // Level XP thresholds
    private static final int[] LEVEL_THRESHOLDS = {0, 100, 250, 500, 1000};
    private static final String[] LEVEL_NAMES = {"Beginner", "Learner", "Scholar", "Expert", "Master"};

    // ─────────────────────────────────────────────────────────────────────────
    // ENTRY POINT — called by QuizService after every quiz
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public List<Achievement> processQuizCompletion(User user, int correctAnswers,
                                                    int totalQuestions, double scorePercentage) {
        // Get or create UserStats
        UserStats stats = userStatsRepository.findByUserId(user.getId())
                .orElse(UserStats.builder().user(user).build());

        // ── Update streak ─────────────────────────────────────────────────────
        boolean streakBonus = updateStreak(stats);

        // ── Calculate XP earned this quiz ─────────────────────────────────────
        int xpEarned = 10                            // base completion XP
                + (correctAnswers * 2)               // correct answer XP
                + (streakBonus ? 5 : 0);             // streak bonus

        // ── Update stats ──────────────────────────────────────────────────────
        stats.setTotalXp(stats.getTotalXp() + xpEarned);
        stats.setTotalQuizzesCompleted(stats.getTotalQuizzesCompleted() + 1);
        stats.setTotalQuestionsAnswered(stats.getTotalQuestionsAnswered() + totalQuestions);
        stats.setTotalCorrectAnswers(stats.getTotalCorrectAnswers() + correctAnswers);
        stats.setLastQuizDate(LocalDate.now());
        stats.setCurrentLevel(calculateLevel(stats.getTotalXp()));

        userStatsRepository.save(stats);

        // ── Check and award achievements ──────────────────────────────────────
        List<Achievement> newlyEarned = checkAndAwardAchievements(
                user, stats, scorePercentage, correctAnswers, totalQuestions);

        // Award bonus XP from badges
        int badgeXp = newlyEarned.stream().mapToInt(Achievement::getXpReward).sum();
        if (badgeXp > 0) {
            stats.setTotalXp(stats.getTotalXp() + badgeXp);
            stats.setCurrentLevel(calculateLevel(stats.getTotalXp()));
            userStatsRepository.save(stats);
        }

        log.info("Gamification updated: userId={}, xpEarned={}, level={}, newBadges={}",
                user.getId(), xpEarned, stats.getCurrentLevel(), newlyEarned.size());

        return newlyEarned;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC — Get gamification profile for current user
    // ─────────────────────────────────────────────────────────────────────────

    public GamificationResponse getProfileForCurrentUser() {
        User user = securityUtils.getCurrentUser();
        return buildProfile(user, List.of(), 0);
    }

    public GamificationResponse getProfileAfterQuiz(User user, List<Achievement> newlyEarned,
                                                     int xpEarnedThisQuiz) {
        return buildProfile(user, newlyEarned, xpEarnedThisQuiz);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC — Leaderboard
    // ─────────────────────────────────────────────────────────────────────────

    public LeaderboardResponse getLeaderboard() {
        User currentUser = securityUtils.getCurrentUser();
        List<UserStats> allStats = userStatsRepository.findTopByXp();
        int totalParticipants = (int) userStatsRepository.count();
        int currentUserRankNum = userStatsRepository.findRankByUserId(currentUser.getId());

        List<LeaderboardResponse.LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(10, allStats.size()); i++) {
            UserStats s = allStats.get(i);
            boolean isMe = s.getUser().getId().equals(currentUser.getId());
            entries.add(LeaderboardResponse.LeaderboardEntry.builder()
                    .rank(i + 1)
                    .name(isMe ? s.getUser().getName() + " (You)" : s.getUser().getName())
                    .totalXp(s.getTotalXp())
                    .currentLevel(s.getCurrentLevel())
                    .levelName(getLevelName(s.getCurrentLevel()))
                    .currentStreak(s.getCurrentStreak())
                    .totalQuizzesCompleted(s.getTotalQuizzesCompleted())
                    .isCurrentUser(isMe)
                    .build());
        }

        // Current user's own rank entry (shown even if not in top 10)
        UserStats myStats = userStatsRepository.findByUserId(currentUser.getId())
                .orElse(UserStats.builder().user(currentUser).build());

        LeaderboardResponse.LeaderboardEntry myEntry = LeaderboardResponse.LeaderboardEntry.builder()
                .rank(currentUserRankNum)
                .name(currentUser.getName() + " (You)")
                .totalXp(myStats.getTotalXp())
                .currentLevel(myStats.getCurrentLevel())
                .levelName(getLevelName(myStats.getCurrentLevel()))
                .currentStreak(myStats.getCurrentStreak())
                .totalQuizzesCompleted(myStats.getTotalQuizzesCompleted())
                .isCurrentUser(true)
                .build();

        return LeaderboardResponse.builder()
                .topUsers(entries)
                .currentUserRank(myEntry)
                .totalParticipants(totalParticipants)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Achievement evaluation
    // ─────────────────────────────────────────────────────────────────────────

    private List<Achievement> checkAndAwardAchievements(User user, UserStats stats,
                                                         double scorePercentage,
                                                         int correctAnswers, int totalQuestions) {
        List<Achievement> allAchievements = achievementRepository.findAll();
        Set<Long> alreadyEarned = userAchievementRepository.findEarnedAchievementIdsByUserId(user.getId());
        List<TopicPerformance> topicPerformances = topicPerformanceRepository
                .findByUserIdOrderByMasteryScoreAsc(user.getId());

        List<Achievement> newlyEarned = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            if (alreadyEarned.contains(achievement.getId())) continue;

            boolean conditionMet = switch (achievement.getConditionType()) {
                case "FIRST_QUIZ" ->
                        stats.getTotalQuizzesCompleted() >= 1;
                case "QUIZ_COUNT" ->
                        stats.getTotalQuizzesCompleted() >= achievement.getConditionValue();
                case "STREAK_DAYS" ->
                        stats.getCurrentStreak() >= achievement.getConditionValue();
                case "XP_THRESHOLD" ->
                        stats.getTotalXp() >= achievement.getConditionValue();
                case "PERFECT_SCORE" ->
                        correctAnswers == totalQuestions && totalQuestions > 0;
                case "MASTERY_TOPIC" ->
                        topicPerformances.stream().anyMatch(tp ->
                                tp.getMasteryScore() != null
                                && tp.getMasteryScore() >= achievement.getConditionValue());
                default -> false;
            };

            if (conditionMet) {
                UserAchievement ua = UserAchievement.builder()
                        .user(user)
                        .achievement(achievement)
                        .build();
                userAchievementRepository.save(ua);
                newlyEarned.add(achievement);
                log.info("Achievement earned: userId={}, badge={}", user.getId(), achievement.getName());
            }
        }

        return newlyEarned;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates streak. Returns true if streak bonus applies.
     *
     * Logic:
     *  - If lastQuizDate == today     → already practiced today, no streak change
     *  - If lastQuizDate == yesterday → streak continues, bonus applies
     *  - If lastQuizDate < yesterday  → streak resets to 1
     *  - If no lastQuizDate           → first time, streak = 1
     */
    private boolean updateStreak(UserStats stats) {
        LocalDate today = LocalDate.now();
        LocalDate last = stats.getLastQuizDate();

        if (last == null) {
            stats.setCurrentStreak(1);
            stats.setLongestStreak(1);
            return false;
        }

        if (last.equals(today)) {
            // Already practiced today — streak doesn't change but no double bonus
            return stats.getCurrentStreak() > 1;
        }

        if (last.equals(today.minusDays(1))) {
            // Practiced yesterday — streak continues
            int newStreak = stats.getCurrentStreak() + 1;
            stats.setCurrentStreak(newStreak);
            if (newStreak > stats.getLongestStreak()) {
                stats.setLongestStreak(newStreak);
            }
            return true; // Streak bonus applies
        }

        // Missed a day — reset streak
        stats.setCurrentStreak(1);
        return false;
    }

    private int calculateLevel(int totalXp) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalXp >= LEVEL_THRESHOLDS[i]) return i + 1;
        }
        return 1;
    }

    private String getLevelName(int level) {
        int idx = Math.min(level - 1, LEVEL_NAMES.length - 1);
        return LEVEL_NAMES[Math.max(0, idx)];
    }

    private GamificationResponse buildProfile(User user, List<Achievement> newlyEarned,
                                               int xpEarnedThisQuiz) {
        UserStats stats = userStatsRepository.findByUserId(user.getId())
                .orElse(UserStats.builder().user(user).build());

        List<UserAchievement> earned = userAchievementRepository.findByUserId(user.getId());
        List<Achievement> allAchievements = achievementRepository.findAll();
        Set<Long> earnedIds = earned.stream()
                .map(ua -> ua.getAchievement().getId()).collect(Collectors.toSet());

        int level = stats.getCurrentLevel();
        int xpInLevel = stats.getTotalXp() - (level <= LEVEL_THRESHOLDS.length
                ? LEVEL_THRESHOLDS[level - 1] : LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1]);
        int xpNeeded = level < LEVEL_THRESHOLDS.length
                ? LEVEL_THRESHOLDS[level] - LEVEL_THRESHOLDS[level - 1] : 999;
        double levelProgress = xpNeeded > 0 ? Math.min(100.0, (double) xpInLevel / xpNeeded * 100) : 100;

        double accuracy = stats.getTotalQuestionsAnswered() > 0
                ? (double) stats.getTotalCorrectAnswers() / stats.getTotalQuestionsAnswered() * 100 : 0;

        List<GamificationResponse.BadgeInfo> earnedBadges = earned.stream()
                .map(ua -> GamificationResponse.BadgeInfo.builder()
                        .name(ua.getAchievement().getName())
                        .description(ua.getAchievement().getDescription())
                        .icon(ua.getAchievement().getIcon())
                        .xpReward(ua.getAchievement().getXpReward())
                        .earnedAt(ua.getEarnedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                        .build())
                .collect(Collectors.toList());

        List<GamificationResponse.BadgeInfo> availableBadges = allAchievements.stream()
                .filter(a -> !earnedIds.contains(a.getId()))
                .map(a -> GamificationResponse.BadgeInfo.builder()
                        .name(a.getName())
                        .description(a.getDescription())
                        .icon(a.getIcon())
                        .xpReward(a.getXpReward())
                        .build())
                .collect(Collectors.toList());

        List<GamificationResponse.BadgeInfo> newBadgeInfos = newlyEarned.stream()
                .map(a -> GamificationResponse.BadgeInfo.builder()
                        .name(a.getName())
                        .description(a.getDescription())
                        .icon(a.getIcon())
                        .xpReward(a.getXpReward())
                        .earnedAt(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                        .build())
                .collect(Collectors.toList());

        return GamificationResponse.builder()
                .totalXp(stats.getTotalXp())
                .currentLevel(level)
                .levelName(getLevelName(level))
                .xpForNextLevel(xpNeeded)
                .xpProgressInLevel(xpInLevel)
                .levelProgressPercent(Math.round(levelProgress * 10.0) / 10.0)
                .currentStreak(stats.getCurrentStreak())
                .longestStreak(stats.getLongestStreak())
                .lastQuizDate(stats.getLastQuizDate())
                .practicedToday(LocalDate.now().equals(stats.getLastQuizDate()))
                .totalQuizzesCompleted(stats.getTotalQuizzesCompleted())
                .totalQuestionsAnswered(stats.getTotalQuestionsAnswered())
                .totalCorrectAnswers(stats.getTotalCorrectAnswers())
                .overallAccuracy(Math.round(accuracy * 10.0) / 10.0)
                .earnedBadges(earnedBadges)
                .availableBadges(availableBadges)
                .totalBadgesEarned(earnedBadges.size())
                .newlyEarnedBadges(newBadgeInfos)
                .xpEarnedThisQuiz(xpEarnedThisQuiz)
                .build();
    }
}
