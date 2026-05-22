package com.telusko.quizapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "questions")   // fixed: was "\"Question\"" — caused issues
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "question_title", nullable = false, length = 1000)
    private String questionTitle;

    @Column(nullable = false)
    private String option1;

    @Column(nullable = false)
    private String option2;

    @Column(nullable = false)
    private String option3;

    @Column(nullable = false)
    private String option4;

    @Column(name = "right_answer", nullable = false)
    private String rightAnswer;

    // Easy / Medium / Hard
    @Column(name = "difficulty_level")
    private String difficultyLevel;

    // e.g. "Java", "Python", "Data Structures"
    private String category;
}
