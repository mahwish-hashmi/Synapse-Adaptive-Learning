package com.telusko.quizapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1000, message = "Message too long — max 1000 characters")
    private String message;
}
