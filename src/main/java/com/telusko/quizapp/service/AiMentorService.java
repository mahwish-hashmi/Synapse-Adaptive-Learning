package com.telusko.quizapp.service;

import com.telusko.quizapp.dto.response.AiChatResponse;
import com.telusko.quizapp.dto.response.AiInsightResponse;
import com.telusko.quizapp.dto.response.WeaknessReportResponse;
import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 *  AI MENTOR SERVICE — Phase 5
 * ═══════════════════════════════════════════════════════════════
 *
 * Provides two AI-powered features:
 *
 * 1. PERSONALISED INSIGHT GENERATION
 *    Sends the student's full TopicPerformance data to OpenAI.
 *    The LLM analyses it and returns deep, contextual feedback.
 *    Example output:
 *      "You have attempted Data Structures 12 times with 38% mastery.
 *       Your accuracy drops sharply on tree traversal questions (23%)
 *       vs array questions (71%). Revise pre-order and in-order DFS
 *       before your next attempt."
 *
 * 2. AI MENTOR CHATBOT
 *    Student asks any learning question.
 *    System prompt includes their performance data as context.
 *    The LLM answers as a personalised tutor — not a generic chatbot.
 *
 * IMPORTANT DESIGN DECISION — Graceful fallback:
 *    If OPENAI_API_KEY is not set or OpenAI call fails,
 *    the service falls back to the rule-based insight engine from Phase 3.
 *    The app NEVER crashes because of a missing API key.
 *    aiAvailable = false is returned so frontend can show a notice.
 *
 * This is production-quality design — third-party APIs fail.
 * Always design AI features to degrade gracefully.
 *
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMentorService {

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    @Value("${openai.max-tokens:500}")
    private int maxTokens;

    private final SecurityUtils securityUtils;
    private final WeakTopicDetectionService weakTopicDetectionService;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 1 — AI-generated performance insight
    // ─────────────────────────────────────────────────────────────────────────

    public AiInsightResponse generatePersonalisedInsight() {
        User user = securityUtils.getCurrentUser();

        // Load current weakness report — this is the context we send to OpenAI
        WeaknessReportResponse report = weakTopicDetectionService.getReportForCurrentUser();

        if (!isAiAvailable()) {
            log.warn("OpenAI key not configured — using rule-based fallback insight");
            return AiInsightResponse.builder()
                    .aiAnalysis(report.getPrimaryInsight())
                    .quickTips(report.getActionableAdvice())
                    .aiAvailable(false)
                    .modelUsed("rule-based-engine")
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // Build the prompt — inject real student data as context
        String systemPrompt = buildMentorSystemPrompt(user.getName());
        String userPrompt = buildInsightPrompt(report);

        try {
            String aiResponse = callOpenAi(systemPrompt, userPrompt);

            // Split response into analysis + tips (model is instructed to use "TIPS:" separator)
            String[] parts = aiResponse.split("TIPS:", 2);
            String analysis = parts[0].trim();
            String tips = parts.length > 1 ? parts[1].trim() : report.getActionableAdvice();

            return AiInsightResponse.builder()
                    .aiAnalysis(analysis)
                    .quickTips(tips)
                    .aiAvailable(true)
                    .modelUsed(openAiModel)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("OpenAI call failed, falling back to rule engine: {}", e.getMessage());
            return AiInsightResponse.builder()
                    .aiAnalysis(report.getPrimaryInsight())
                    .quickTips(report.getActionableAdvice())
                    .aiAvailable(false)
                    .modelUsed("rule-based-engine (fallback)")
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 2 — AI Mentor Chatbot
    // ─────────────────────────────────────────────────────────────────────────

    public AiChatResponse chat(String userMessage) {
        User user = securityUtils.getCurrentUser();

        if (!isAiAvailable()) {
            return AiChatResponse.builder()
                    .userMessage(userMessage)
                    .aiReply("The AI Mentor is not configured yet. Please set your OPENAI_API_KEY " +
                             "environment variable to enable AI chat. In the meantime, check your " +
                             "weakness report for personalized recommendations.")
                    .aiAvailable(false)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Inject student's performance context into system prompt
        WeaknessReportResponse report = weakTopicDetectionService.getReportForCurrentUser();
        String systemPrompt = buildChatSystemPrompt(user.getName(), report);

        try {
            String aiReply = callOpenAi(systemPrompt, userMessage);
            return AiChatResponse.builder()
                    .userMessage(userMessage)
                    .aiReply(aiReply)
                    .aiAvailable(true)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("OpenAI chat failed: {}", e.getMessage());
            return AiChatResponse.builder()
                    .userMessage(userMessage)
                    .aiReply("I'm having trouble connecting right now. Please try again in a moment.")
                    .aiAvailable(false)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — OpenAI API call (using RestTemplate — no extra SDK needed)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calls OpenAI Chat Completions API.
     *
     * Uses RestTemplate (already in Spring Boot) — no extra client needed.
     * Sends: system prompt (context) + user prompt (question)
     * Returns: the model's text response
     */
    @SuppressWarnings("unchecked")
    private String callOpenAi(String systemPrompt, String userMessage) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        // Build request body as a Map — avoids needing the OpenAI SDK
        Map<String, Object> requestBody = Map.of(
            "model", openAiModel,
            "max_tokens", maxTokens,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_URL, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message =
                        (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }

        throw new RuntimeException("Empty response from OpenAI");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Prompt builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * System prompt for insight generation.
     * This is what makes the AI act as a tutor, not a generic assistant.
     */
    private String buildMentorSystemPrompt(String studentName) {
        return String.format(
            "You are an expert AI learning mentor for a programming education platform. " +
            "Your student's name is %s. " +
            "You have access to their full performance data. " +
            "Analyse their weaknesses deeply and give specific, actionable advice. " +
            "Be encouraging but honest. Keep your response under 150 words. " +
            "Format: First write a 2-3 sentence analysis paragraph. " +
            "Then write 'TIPS:' followed by 3 bullet points of specific actions they should take.",
            studentName
        );
    }

    /**
     * Converts the student's WeaknessReportResponse into a structured prompt.
     * This is the key — we pass real data, not just the question.
     */
    private String buildInsightPrompt(WeaknessReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here is my current performance data:\n\n");
        sb.append(String.format("Overall Mastery Score: %.1f/100\n", report.getOverallScore()));
        sb.append(String.format("Strongest Topic: %s\n",
                report.getStrongestTopic() != null ? report.getStrongestTopic() : "None yet"));
        sb.append(String.format("Weakest Topic: %s\n",
                report.getWeakestTopic() != null ? report.getWeakestTopic() : "None yet"));
        sb.append(String.format("Total Quizzes Taken: %d\n\n", report.getTotalQuizzesTaken()));

        if (report.getAllTopics() != null && !report.getAllTopics().isEmpty()) {
            sb.append("Topic Breakdown:\n");
            report.getAllTopics().forEach(t ->
                sb.append(String.format("  - %s: %.1f%% mastery, %d attempts, trend: %s%s\n",
                    t.getCategory(),
                    t.getMasteryScore() != null ? t.getMasteryScore() : 0.0,
                    t.getTotalAttempts() != null ? t.getTotalAttempts() : 0,
                    t.getTrend() != null ? t.getTrend() : "STABLE",
                    Boolean.TRUE.equals(t.getIsWeak()) ? " [WEAK]" : ""))
            );
        }

        sb.append("\nPlease analyse my performance and give me specific advice.");
        return sb.toString();
    }

    /**
     * System prompt for chatbot — injects performance context so AI
     * can answer questions relative to the student's actual situation.
     */
    private String buildChatSystemPrompt(String studentName, WeaknessReportResponse report) {
        String weakTopics = report.getWeakTopics() != null
                ? report.getWeakTopics().stream()
                        .map(t -> t.getCategory())
                        .collect(Collectors.joining(", "))
                : "none identified yet";

        return String.format(
            "You are an expert AI learning mentor for a programming education platform. " +
            "You are talking to %s. " +
            "Their current overall mastery score is %.1f/100. " +
            "Their weak topics are: %s. " +
            "Their strongest topic is: %s. " +
            "Answer their questions as a knowledgeable, encouraging tutor. " +
            "Keep responses focused, practical and under 200 words. " +
            "If they ask about a topic they are weak in, acknowledge it and explain clearly.",
            studentName,
            report.getOverallScore() != null ? report.getOverallScore() : 0.0,
            weakTopics,
            report.getStrongestTopic() != null ? report.getStrongestTopic() : "not determined yet"
        );
    }

    private boolean isAiAvailable() {
        return openAiApiKey != null
                && !openAiApiKey.isBlank()
                && !openAiApiKey.equals("your-openai-api-key-here");
    }
}
