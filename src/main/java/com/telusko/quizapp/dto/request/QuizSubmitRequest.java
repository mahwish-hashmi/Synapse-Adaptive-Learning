package com.telusko.quizapp.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QuizSubmitRequest {

    @NotNull(message = "Quiz attempt ID is required")
    private Long quizAttemptId;

    /**
     * Map of questionId → selectedAnswer
     * e.g. { "5": "ArrayList", "6": "HashMap", ... }
     * If a question was skipped, still include it with null value.
     */
    @NotEmpty(message = "Answers cannot be empty")
    private Map<Integer, String> answers;

    /**
     * Optional: map of questionId → seconds spent on that question
     * Used for confidence scoring in Phase 3
     * e.g. { "5": 12, "6": 45 }
     */
    private Map<Integer, Long> timingsPerQuestion;
}
