package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.dto.CreateSongRequest;
import com.aremi.musicstreamingservice.model.Song;
import com.aremi.musicstreamingservice.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;

    public Mono<Void> saveSong(CreateSongRequest request) {
        Path path = Paths.get(request.getFilePath());

        if(!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("File audio non trovato o non valido: {}", request.getFilePath());
            return Mono.error(new IllegalArgumentException("Il file audio non esiste o non è valido"));
        }

        Song song = Song.builder()
                .title(request.getTitle())
                .artist(request.getArtist())
                .genre(request.getGenre())
                .filePath(request.getFilePath())
                .build();

        return songRepository.save(song)
                .doOnSuccess(saved -> log.info("Canzone salvata con id {}", saved.getId()))
                .then();
    }
}
