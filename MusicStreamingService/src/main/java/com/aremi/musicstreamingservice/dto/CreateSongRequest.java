package com.aremi.musicstreamingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSongRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String artist;
    @NotBlank
    private String genre;
    @NotBlank
    private String filePath;
}
