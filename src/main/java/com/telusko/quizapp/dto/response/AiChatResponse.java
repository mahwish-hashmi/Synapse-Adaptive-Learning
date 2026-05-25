package com.telusko.quizapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String userMessage;
    private String aiReply;
    private boolean aiAvailable;    // Falls back gracefully if OpenAI key not set
    private LocalDateTime timestamp;
}
