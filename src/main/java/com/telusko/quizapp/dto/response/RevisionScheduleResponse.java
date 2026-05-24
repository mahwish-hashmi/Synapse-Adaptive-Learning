package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionScheduleResponse {

    private String category;
    private LocalDate nextRevisionDate;
    private LocalDate lastPracticedDate;
    private Integer intervalDays;
    private Integer consecutiveCorrectSessions;
    private Boolean isDueToday;
    private String priority;              // HIGH / MEDIUM / LOW
    private long daysUntilDue;           // negative = overdue
}
