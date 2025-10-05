package com.aremi.musicstreamingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginPreloadRequest {
    @NotBlank(message = "UserId è obbligatorio")
    private String userId;      // es: "abc123"
    private String timestamp;   // es: "2025-10-05T18:00:00Z"
}
