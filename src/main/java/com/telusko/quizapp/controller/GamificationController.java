package com.telusko.quizapp.controller;

import com.telusko.quizapp.dto.response.ApiResponse;
import com.telusko.quizapp.dto.response.GamificationResponse;
import com.telusko.quizapp.dto.response.LeaderboardResponse;
import com.telusko.quizapp.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "XP, levels, streaks, badges and leaderboard")
@SecurityRequirement(name = "bearerAuth")
public class GamificationController {

    private final GamificationService gamificationService;

    /**
     * GET /api/v1/gamification/profile
     *
     * Returns the full gamification profile:
     *   XP, level, level progress bar, streak, all earned badges,
     *   badges not yet earned (to show as locked), overall accuracy
     */
    @GetMapping("/profile")
    @Operation(summary = "Get your full gamification profile — XP, level, streaks, badges")
    public ResponseEntity<ApiResponse<GamificationResponse>> getProfile() {
        GamificationResponse profile = gamificationService.getProfileForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Gamification profile fetched", profile));
    }

    /**
     * GET /api/v1/gamification/leaderboard
     *
     * Top 10 students by XP + current user's rank even if outside top 10.
     * Names are only shown — no sensitive data exposed.
     */
    @GetMapping("/leaderboard")
    @Operation(summary = "View the top 10 leaderboard and your own rank")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard() {
        LeaderboardResponse leaderboard = gamificationService.getLeaderboard();
        return ResponseEntity.ok(ApiResponse.success("Leaderboard fetched", leaderboard));
    }
}
