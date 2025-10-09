package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.dto.CreateSongRequest;
import com.aremi.musicstreamingservice.dto.CreateUserStateRequest;
import com.aremi.musicstreamingservice.model.Song;
import com.aremi.musicstreamingservice.model.UserState;
import com.aremi.musicstreamingservice.repository.SongRepository;
import com.aremi.musicstreamingservice.repository.UserStateRepository;
import com.netflix.appinfo.ApplicationInfoManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final UserStateRepository userStateRepository;

    public Mono<Song> saveSong(CreateSongRequest request) {
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
                .doOnError(ex -> log.error("Errore salvataggio canzone, request {}", request, ex));
    }

    public Mono<List<Song>> getAllSongs() {
        return songRepository.findAll()
                .collectList();
    }

    public Mono<UserState> saveUserState(@Valid CreateUserStateRequest request) {
        UserState userState = UserState.builder()
                .userId(request.getUserId())
                .activeSongId(request.getActiveSongId())
                .build();

        return userStateRepository.save(userState)
                .doOnSuccess(saved -> log.info("UserState salvato successo"))
                .doOnError(ex -> log.error("Errore salvataggio UserState, request {}", request, ex));
    }

    public Mono<Void> deleteAllUserState() {
        return userStateRepository.deleteAll()
                .doOnSuccess(saved -> log.info("Delete All UserState eseguita con successo"))
                .doOnError(ex -> log.error("Errore durante Delete All UserState", ex));
    }

    public Mono<List<UserState>> getAllUserState() {
        return userStateRepository.findAll()
                .collectList();
    }
}
