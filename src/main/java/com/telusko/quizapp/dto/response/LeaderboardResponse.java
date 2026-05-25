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
public class LeaderboardResponse {

    private List<LeaderboardEntry> topUsers;
    private LeaderboardEntry currentUserRank;  // Current user's position even if not in top 10
    private int totalParticipants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private int rank;
        private String name;
        private Integer totalXp;
        private Integer currentLevel;
        private String levelName;
        private Integer currentStreak;
        private Integer totalQuizzesCompleted;
        private boolean isCurrentUser;
    }
}
