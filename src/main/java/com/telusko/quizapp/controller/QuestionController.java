package com.telusko.quizapp.controller;

import com.telusko.quizapp.dto.response.ApiResponse;
import com.telusko.quizapp.entity.Question;
import com.telusko.quizapp.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Manage quiz questions")
@SecurityRequirement(name = "bearerAuth")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @Operation(summary = "Get all questions")
    public ResponseEntity<ApiResponse<List<Question>>> getAllQuestions() {
        List<Question> questions = questionService.getAllQuestions();
        return ResponseEntity.ok(ApiResponse.success("Questions fetched successfully", questions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get question by ID")
    public ResponseEntity<ApiResponse<Question>> getQuestionById(@PathVariable Integer id) {
        Question question = questionService.getQuestionById(id);
        return ResponseEntity.ok(ApiResponse.success("Question fetched", question));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get questions by category")
    public ResponseEntity<ApiResponse<List<Question>>> getByCategory(
            @PathVariable String category) {
        List<Question> questions = questionService.getQuestionsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success("Questions fetched", questions));
    }

    @GetMapping("/category/{category}/difficulty/{difficulty}")
    @Operation(summary = "Get questions by category and difficulty")
    public ResponseEntity<ApiResponse<List<Question>>> getByCategoryAndDifficulty(
            @PathVariable String category,
            @PathVariable String difficulty) {
        List<Question> questions =
                questionService.getQuestionsByCategoryAndDifficulty(category, difficulty);
        return ResponseEntity.ok(ApiResponse.success("Questions fetched", questions));
    }

    // ── Admin only ────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Add a new question (Admin only)")
    public ResponseEntity<ApiResponse<Question>> addQuestion(
            @Valid @RequestBody Question question) {
        Question saved = questionService.addQuestion(question);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question added successfully", saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a question (Admin only)")
    public ResponseEntity<ApiResponse<Question>> updateQuestion(
            @PathVariable Integer id,
            @Valid @RequestBody Question question) {
        Question updated = questionService.updateQuestion(id, question);
        return ResponseEntity.ok(ApiResponse.success("Question updated", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a question (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Integer id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("Question deleted"));
    }
}
