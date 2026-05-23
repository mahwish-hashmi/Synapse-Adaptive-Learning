package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {

    // All attempts by user for a specific category — Phase 3 accuracy calculation
    List<QuestionAttempt> findByUserIdAndCategory(Long userId, String category);

    // All attempts by a user — for full analytics dashboard
    List<QuestionAttempt> findByUserId(Long userId);

    // How many times has a user attempted a specific question
    int countByUserIdAndQuestionId(Long userId, Integer questionId);

    /**
     * Per-category accuracy summary for a user.
     * Returns: [category, totalAttempts, correctCount, avgTimeSeconds]
     * Used directly by Phase 3 WeakTopicDetectionService.
     */
    @Query("SELECT qa.category, " +
           "COUNT(qa) as totalAttempts, " +
           "SUM(CASE WHEN qa.isCorrect = true THEN 1 ELSE 0 END) as correctCount, " +
           "AVG(qa.timeTakenSeconds) as avgTime " +
           "FROM QuestionAttempt qa " +
           "WHERE qa.user.id = :userId " +
           "GROUP BY qa.category")
    List<Object[]> findCategoryPerformanceSummary(Long userId);

    /**
     * Recent accuracy trend for a category (last N attempts).
     * Used to detect if user is improving or declining.
     */
    @Query("SELECT qa FROM QuestionAttempt qa " +
           "WHERE qa.user.id = :userId AND qa.category = :category " +
           "ORDER BY qa.answeredAt DESC")
    List<QuestionAttempt> findRecentByUserIdAndCategory(Long userId, String category);

    // Questions this user consistently gets wrong — for targeted revision
    @Query("SELECT qa.question.id, COUNT(qa) as wrongCount " +
           "FROM QuestionAttempt qa " +
           "WHERE qa.user.id = :userId AND qa.isCorrect = false " +
           "GROUP BY qa.question.id " +
           "ORDER BY wrongCount DESC")
    List<Object[]> findMostFailedQuestionIds(Long userId);
}
