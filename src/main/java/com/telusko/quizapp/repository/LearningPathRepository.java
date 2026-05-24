package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    Optional<LearningPath> findByUserId(Long userId);
}
