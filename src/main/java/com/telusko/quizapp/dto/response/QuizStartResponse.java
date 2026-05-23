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
public class QuizStartResponse {

    private Long quizAttemptId;
    private String category;
    private int totalQuestions;

    // Questions WITHOUT correct answers — never expose rightAnswer to client
    private List<QuestionResponse> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResponse {
        private Integer id;
        private String questionTitle;
        private String option1;
        private String option2;
        private String option3;
        private String option4;
        private String difficultyLevel;
        private String category;
        // rightAnswer is intentionally NOT here
    }
}
