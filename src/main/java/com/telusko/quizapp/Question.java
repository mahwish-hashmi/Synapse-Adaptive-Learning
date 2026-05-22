package com.telusko.quizapp;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "\"Question\"")
@JsonPropertyOrder({
        "id",
        "questionTitle",
        "option1",
        "option2",
        "option3",
        "option4",
        "rightAnswer",
        "difficultyLevel",
        "category"
})
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "question_title")
    private String questionTitle;

    private String option1;
    private String option2;
    private String option3;
    private String option4;

    @Column(name = "right_answer")
    private String rightAnswer;

    @Column(name = "difficulty_level")
    private String difficultyLevel;

    private String category;

}
