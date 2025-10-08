package com.aremi.musicstreamingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("user_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserState {
    @Id
    private String userId;
    private String activeSongId;
    private LocalDateTime lastUpdate;
}
