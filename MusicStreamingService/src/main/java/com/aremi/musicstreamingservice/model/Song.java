package com.aremi.musicstreamingservice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("songs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Song {
    @Id
    private String id;

    // --- Metadata
    @NotBlank(message = "Il titolo è obbligatorio")
    private String title;
    @NotBlank(message = "L'artista è obbligatorio")
    private String artist;
    @NotBlank(message = "Il genere è obbligatorio")
    private String genre;

    // --- Statistics
    @Builder.Default
    private Long playCount = 0L;

    // --- Media
    @NotBlank(message = "Il filePath è obbligatorio")
    private String filePath;
}
