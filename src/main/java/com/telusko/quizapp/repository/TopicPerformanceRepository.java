package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.TopicPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicPerformanceRepository extends JpaRepository<TopicPerformance, Long> {

    // Find existing row for a user+category — for upsert logic
    Optional<TopicPerformance> findByUserIdAndCategory(Long userId, String category);

    // All topics for a user, sorted by mastery ascending (weakest first)
    List<TopicPerformance> findByUserIdOrderByMasteryScoreAsc(Long userId);

    // Only the topics flagged as weak
    List<TopicPerformance> findByUserIdAndIsWeakTrue(Long userId);

    // Strong topics (mastery >= 75)
    @Query("SELECT tp FROM TopicPerformance tp WHERE tp.user.id = :userId AND tp.masteryScore >= 75")
    List<TopicPerformance> findStrongTopicsByUserId(Long userId);

    // Weakest single topic (lowest mastery, minimum 3 attempts)
    @Query("SELECT tp FROM TopicPerformance tp WHERE tp.user.id = :userId " +
           "AND tp.totalAttempts >= 3 ORDER BY tp.masteryScore ASC")
    List<TopicPerformance> findWeakestTopicByUserId(Long userId);

    // Strongest single topic
    @Query("SELECT tp FROM TopicPerformance tp WHERE tp.user.id = :userId " +
           "AND tp.totalAttempts >= 3 ORDER BY tp.masteryScore DESC")
    List<TopicPerformance> findStrongestTopicByUserId(Long userId);
}
