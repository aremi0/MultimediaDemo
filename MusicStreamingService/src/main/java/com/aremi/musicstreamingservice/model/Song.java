package com.aremi.musicstreamingservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "songs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Song {
    @Id
    private String id;

    // --- Metadata
    private String title;
    private String artist;
    private String genre;

    // --- Statistics
    private Long playCount;

    // --- Media
    private String filePath;
}
