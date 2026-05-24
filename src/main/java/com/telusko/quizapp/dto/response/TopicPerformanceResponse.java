package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicPerformanceResponse {

    private String category;
    private Integer totalAttempts;
    private Integer correctAnswers;
    private Double accuracyScore;
    private Double masteryScore;
    private Double avgTimeTakenSeconds;
    private Boolean isWeak;
    private String trend;           // IMPROVING / DECLINING / STABLE
    private String masteryLabel;    // Beginner / Developing / Proficient / Expert
    private LocalDateTime lastAttemptedAt;
}
