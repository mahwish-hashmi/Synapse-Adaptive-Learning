package com.telusko.quizapp.controller;

import com.telusko.quizapp.dto.response.ApiResponse;
import com.telusko.quizapp.dto.response.WeaknessReportResponse;
import com.telusko.quizapp.entity.User;
import com.telusko.quizapp.security.SecurityUtils;
import com.telusko.quizapp.service.WeakTopicDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Weak topic detection and performance insights")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final WeakTopicDetectionService weakTopicDetectionService;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/v1/analytics/weakness-report
     *
     * The main dashboard endpoint.
     * Returns: overall score, weak topics, strong topics,
     * AI insight message, actionable advice, per-topic breakdown.
     *
     * Call this on the student home screen after every quiz.
     */
    @GetMapping("/weakness-report")
    @Operation(summary = "Get your full AI-generated weakness analysis report")
    public ResponseEntity<ApiResponse<WeaknessReportResponse>> getWeaknessReport() {
        WeaknessReportResponse report = weakTopicDetectionService.getReportForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Weakness report generated", report));
    }

    /**
     * POST /api/v1/analytics/recalculate
     *
     * Manually triggers a full recalculation across ALL topics for the current user.
     * Useful during testing. In production this runs automatically after every quiz.
     */
    @PostMapping("/recalculate")
    @Operation(summary = "Manually trigger weakness analysis recalculation across all topics")
    public ResponseEntity<ApiResponse<WeaknessReportResponse>> recalculate() {
        User currentUser = securityUtils.getCurrentUser();

        // Re-run analysis for every topic this user has attempted
        weakTopicDetectionService.regenerateWeaknessReport(currentUser);

        WeaknessReportResponse report = weakTopicDetectionService.getReportForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Analysis recalculated successfully", report));
    }

    /**
     * GET /api/v1/analytics/user/{userId}/weakness-report
     * Admin only — view any student's weakness report.
     */
    @GetMapping("/user/{userId}/weakness-report")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get weakness report for a specific user (Admin only)")
    public ResponseEntity<ApiResponse<WeaknessReportResponse>> getReportForUser(
            @PathVariable Long userId) {
        WeaknessReportResponse report = weakTopicDetectionService.getReportForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Report fetched", report));
    }
}
