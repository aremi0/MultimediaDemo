package com.aremi.musicstreamingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserStateRequest {
    @NotBlank(message = "userId è obbligatorio")
    private String userId;

    @NotBlank(message = "activeSongId è obbligatorio")
    private String activeSongId;;
}
