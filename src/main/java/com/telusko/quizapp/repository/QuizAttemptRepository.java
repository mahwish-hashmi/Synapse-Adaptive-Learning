package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    // All completed attempts by a user, newest first
    List<QuizAttempt> findByUserIdAndStatusOrderByStartedAtDesc(
            Long userId, QuizAttempt.Status status);

    // All attempts by a user (any status)
    List<QuizAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    // Attempts by user + category — used in Phase 3 per-topic analytics
    List<QuizAttempt> findByUserIdAndCategoryAndStatus(
            Long userId, String category, QuizAttempt.Status status);

    // Fetch with question attempts in one query — avoids N+1 problem
    @Query("SELECT qa FROM QuizAttempt qa " +
           "LEFT JOIN FETCH qa.questionAttempts " +
           "WHERE qa.id = :id")
    Optional<QuizAttempt> findByIdWithQuestionAttempts(Long id);

    // Average score across all completed quizzes for a user
    @Query("SELECT AVG(qa.scorePercentage) FROM QuizAttempt qa " +
           "WHERE qa.user.id = :userId AND qa.status = 'COMPLETED'")
    Double findAverageScoreByUserId(Long userId);

    // Count of completed attempts
    long countByUserIdAndStatus(Long userId, QuizAttempt.Status status);
}
