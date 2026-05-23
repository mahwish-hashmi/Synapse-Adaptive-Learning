package com.telusko.quizapp.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuizStartRequest {

    @NotBlank(message = "Category is required")
    private String category;

    // How many questions in this quiz session
    @Min(value = 1, message = "Minimum 1 question")
    @Max(value = 50, message = "Maximum 50 questions per quiz")
    private int numberOfQuestions = 10;

    // Optional: "Easy", "Medium", "Hard", or null for mixed
    private String difficultyLevel;
}
