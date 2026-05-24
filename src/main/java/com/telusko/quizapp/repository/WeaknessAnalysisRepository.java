package com.telusko.quizapp.repository;

import com.telusko.quizapp.entity.WeaknessAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeaknessAnalysisRepository extends JpaRepository<WeaknessAnalysis, Long> {

    Optional<WeaknessAnalysis> findByUserId(Long userId);
}
