package com.telusko.quizapp.controller;

import com.telusko.quizapp.dto.response.ApiResponse;
import com.telusko.quizapp.dto.response.LearningPathResponse;
import com.telusko.quizapp.service.LearningPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learning-path")
@RequiredArgsConstructor
@Tag(name = "Learning Path", description = "Personalized learning paths, adaptive quiz recommendations, revision schedule")
@SecurityRequirement(name = "bearerAuth")
public class LearningPathController {

    private final LearningPathService learningPathService;

    /**
     * GET /api/v1/learning-path
     *
     * Main learning dashboard endpoint.
     * Returns:
     *   - Recommended next topic + difficulty
     *   - Ordered topic sequence
     *   - Topics due for revision today
     *   - Upcoming revisions in next 7 days
     *   - Adaptive quiz recommendation (what to start next)
     *   - Estimated days to reach mastery
     *   - Current learning phase (FOUNDATION / DEVELOPMENT / MASTERY / ADVANCED)
     */
    @GetMapping
    @Operation(summary = "Get your personalized learning path and adaptive quiz recommendation")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getLearningPath() {
        LearningPathResponse response = learningPathService.getLearningPathForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Learning path fetched", response));
    }

    /**
     * GET /api/v1/learning-path/user/{userId}
     * Admin only — view any student's learning path.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get learning path for a specific user (Admin only)")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getLearningPathForUser(
            @PathVariable Long userId) {
        LearningPathResponse response = learningPathService.getLearningPathForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Learning path fetched", response));
    }
}
