package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizHistoryResponse {

    private int totalAttempts;
    private double averageScore;
    private List<AttemptSummary> attempts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttemptSummary {
        private Long attemptId;
        private String category;
        private int totalQuestions;
        private int correctAnswers;
        private double scorePercentage;
        private long timeTakenSeconds;
        private String status;
        private LocalDateTime completedAt;
    }
}
