package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {

    private Long quizAttemptId;
    private String category;

    private int totalQuestions;
    private int correctAnswers;
    private int incorrectAnswers;
    private double scorePercentage;
    private long timeTakenSeconds;

    // Performance label: "Excellent" / "Good" / "Needs Practice" / "Keep Trying"
    private String performanceLabel;

    // Encouragement/feedback message
    private String message;

    // Per-question breakdown so user knows what they got wrong
    private List<QuestionResultDetail> questionResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResultDetail {
        private Integer questionId;
        private String questionTitle;
        private String selectedAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private long timeTakenSeconds;
    }
}
