package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response from the AI Mentor for performance analysis.
 *
 * Contains both:
 *   - aiAnalysis  → LLM-generated deep insight about performance
 *   - quickTips   → Bullet-point actionable suggestions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightResponse {

    private String aiAnalysis;      // Full LLM-generated insight paragraph
    private String quickTips;       // Concise actionable suggestions
    private String modelUsed;       // Which model generated this
    private boolean aiAvailable;    // False if OpenAI key not configured — falls back to rule engine
    private LocalDateTime generatedAt;
}
