package com.telusko.quizapp.controller;

import com.telusko.quizapp.dto.request.QuizStartRequest;
import com.telusko.quizapp.dto.request.QuizSubmitRequest;
import com.telusko.quizapp.dto.response.ApiResponse;
import com.telusko.quizapp.dto.response.QuizHistoryResponse;
import com.telusko.quizapp.dto.response.QuizResultResponse;
import com.telusko.quizapp.dto.response.QuizStartResponse;
import com.telusko.quizapp.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
@Tag(name = "Quiz Engine", description = "Start quizzes, submit answers, view results")
@SecurityRequirement(name = "bearerAuth")
public class QuizController {

    private final QuizService quizService;

    /**
     * POST /api/v1/quiz/start
     *
     * Body: { "category": "Java", "numberOfQuestions": 10, "difficultyLevel": "Medium" }
     *
     * Returns: attemptId + questions (no correct answers)
     * Save the attemptId — you need it to submit.
     */
    @PostMapping("/start")
    @Operation(summary = "Start a new quiz session",
               description = "Returns questions WITHOUT correct answers. Save the quizAttemptId.")
    public ResponseEntity<ApiResponse<QuizStartResponse>> startQuiz(
            @Valid @RequestBody QuizStartRequest request) {

        QuizStartResponse response = quizService.startQuiz(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz started successfully", response));
    }

    /**
     * POST /api/v1/quiz/submit
     *
     * Body: {
     *   "quizAttemptId": 1,
     *   "answers": { "5": "ArrayList", "6": "HashMap" },
     *   "timingsPerQuestion": { "5": 12, "6": 45 }
     * }
     *
     * Returns: full result with score and per-question breakdown
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit quiz answers and get scored result")
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitQuiz(
            @Valid @RequestBody QuizSubmitRequest request) {

        QuizResultResponse result = quizService.submitQuiz(request);
        return ResponseEntity.ok(ApiResponse.success("Quiz submitted successfully", result));
    }

    /**
     * GET /api/v1/quiz/result/{attemptId}
     * View the result of any past attempt.
     */
    @GetMapping("/result/{attemptId}")
    @Operation(summary = "Get result of a specific quiz attempt")
    public ResponseEntity<ApiResponse<QuizResultResponse>> getResult(
            @PathVariable Long attemptId) {

        QuizResultResponse result = quizService.getResult(attemptId);
        return ResponseEntity.ok(ApiResponse.success("Result fetched", result));
    }

    /**
     * GET /api/v1/quiz/history
     * All past quiz attempts for the logged-in user.
     */
    @GetMapping("/history")
    @Operation(summary = "Get quiz history for the current user")
    public ResponseEntity<ApiResponse<QuizHistoryResponse>> getHistory() {
        QuizHistoryResponse history = quizService.getHistory();
        return ResponseEntity.ok(ApiResponse.success("History fetched", history));
    }
}
