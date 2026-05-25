package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    Optional<UserStats> findByUserId(Long userId);

    // Top N users by XP — for leaderboard
    @Query("SELECT us FROM UserStats us ORDER BY us.totalXp DESC")
    List<UserStats> findTopByXp();

    // Find rank of a specific user (how many users have more XP)
    @Query("SELECT COUNT(us) + 1 FROM UserStats us WHERE us.totalXp > " +
           "(SELECT u2.totalXp FROM UserStats u2 WHERE u2.user.id = :userId)")
    int findRankByUserId(Long userId);
}
