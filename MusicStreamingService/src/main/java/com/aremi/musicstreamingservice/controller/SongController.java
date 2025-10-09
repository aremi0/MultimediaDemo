package com.aremi.musicstreamingservice.controller;

import com.aremi.common.logging.annotation.Monitor;
import com.aremi.musicstreamingservice.dto.CreateSongRequest;
import com.aremi.musicstreamingservice.dto.CreateUserStateRequest;
import com.aremi.musicstreamingservice.model.Song;
import com.aremi.musicstreamingservice.model.UserState;
import com.aremi.musicstreamingservice.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/public/songs")
@RequiredArgsConstructor
@Validated
@Monitor
public class SongController {

    private final SongService songService;

    @PostMapping
    public Mono<ResponseEntity<Song>> createSong(@Valid @RequestBody CreateSongRequest request) {
        log.info("Richiesta creazione nuova canzone: {}", request);
        return songService.saveSong(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Mono<ResponseEntity<List<Song>>> getAllSongs() {
        log.info("Richiesta listing tutte le canzoni");
        return songService.getAllSongs()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/user-state")
    public Mono<ResponseEntity<UserState>> createUserState(@Valid @RequestBody CreateUserStateRequest request) {
        log.info("Richiesta creazione nuova userState: {}", request);
        return songService.saveUserState(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/user-state")
    public Mono<ResponseEntity<List<UserState>>> getAllUserState() {
        log.info("Richiesta listing tutti gli UserState");
        return songService.getAllUserState()
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/user-state")
    public Mono<ResponseEntity<Void>> deleteAllUserState() {
        log.info("Richiesta eliminazione all userState");
        return songService.deleteAllUserState()
                .thenReturn(ResponseEntity.ok().build());
    }
}
