package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tells the frontend exactly what quiz parameters to use for the next session.
 *
 * The frontend can use this to auto-populate the quiz start form:
 *   category         → pre-select the category
 *   difficulty       → pre-select the difficulty
 *   questionCount    → pre-fill number of questions
 *   reason           → show the student WHY this is recommended
 *   quizType         → REVISION / PROGRESSION / CHALLENGE / NEW_TOPIC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveQuizRecommendation {

    private String category;
    private String difficulty;         // Easy / Medium / Hard
    private int questionCount;
    private String quizType;           // REVISION / PROGRESSION / CHALLENGE / NEW_TOPIC
    private String reason;             // Human-readable explanation shown to student
}
