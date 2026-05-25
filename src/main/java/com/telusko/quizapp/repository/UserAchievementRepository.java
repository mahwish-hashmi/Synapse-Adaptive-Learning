package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(Long userId);

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    // IDs of all achievements already earned by a user — for quick check
    @Query("SELECT ua.achievement.id FROM UserAchievement ua WHERE ua.user.id = :userId")
    Set<Long> findEarnedAchievementIdsByUserId(Long userId);
}
