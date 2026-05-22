package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByCategory(String category);

    // Used by adaptive quiz engine (Phase 4) to pick questions by category + difficulty
    List<Question> findByCategoryAndDifficultyLevel(String category, String difficultyLevel);

    // Random question selection for quiz generation — native query for performance
    @Query(value = "SELECT * FROM questions WHERE category = :category ORDER BY RANDOM() LIMIT :limit",
           nativeQuery = true)
    List<Question> findRandomByCategory(String category, int limit);

    @Query(value = "SELECT * FROM questions WHERE category = :category AND difficulty_level = :difficulty ORDER BY RANDOM() LIMIT :limit",
           nativeQuery = true)
    List<Question> findRandomByCategoryAndDifficulty(String category, String difficulty, int limit);

    List<Question> findByDifficultyLevel(String difficultyLevel);
}
