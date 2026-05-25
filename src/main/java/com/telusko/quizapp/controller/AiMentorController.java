package com.telusko.quizapp.controller;

import com.telusko.quizapp.dto.request.AiChatRequest;
import com.telusko.quizapp.dto.response.AiChatResponse;
import com.telusko.quizapp.dto.response.AiInsightResponse;
import com.telusko.quizapp.dto.response.ApiResponse;
import com.telusko.quizapp.service.AiMentorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Mentor", description = "AI-powered insights and chatbot")
@SecurityRequirement(name = "bearerAuth")
public class AiMentorController {

    private final AiMentorService aiMentorService;

    /**
     * GET /api/v1/ai/insight
     *
     * Generates an LLM-powered deep analysis of the student's performance.
     * If OPENAI_API_KEY is not set, falls back to rule-based engine gracefully.
     * aiAvailable flag in response tells frontend which mode is active.
     */
    @GetMapping("/insight")
    @Operation(summary = "Get AI-generated personalised performance insight")
    public ResponseEntity<ApiResponse<AiInsightResponse>> getInsight() {
        AiInsightResponse insight = aiMentorService.generatePersonalisedInsight();
        return ResponseEntity.ok(ApiResponse.success("AI insight generated", insight));
    }

    /**
     * POST /api/v1/ai/chat
     *
     * Chat with the AI Mentor. The AI knows your performance data.
     * Ask anything: "How do I improve in Data Structures?",
     *               "What should I study today?",
     *               "Explain tree traversal to me"
     *
     * Body: { "message": "Why do I keep failing tree questions?" }
     */
    @PostMapping("/chat")
    @Operation(summary = "Chat with AI Mentor — asks are answered with your performance context")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiMentorService.chat(request.getMessage());
        return ResponseEntity.ok(ApiResponse.success("AI response received", response));
    }
}
