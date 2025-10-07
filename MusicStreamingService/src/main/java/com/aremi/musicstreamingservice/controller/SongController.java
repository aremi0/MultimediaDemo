package com.aremi.musicstreamingservice.controller;

import com.aremi.common.logging.annotation.Monitor;
import com.aremi.musicstreamingservice.dto.CreateSongRequest;
import com.aremi.musicstreamingservice.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/public/songs")
@RequiredArgsConstructor
@Validated
@Monitor
public class SongController {

    private final SongService songService;

    @PostMapping
    public Mono<ResponseEntity<Void>> createSong(@Valid @RequestBody CreateSongRequest request) {
        log.info("Richiesta creazione nuova canzone: {}", request);
        return songService.saveSong(request)
                .thenReturn(ResponseEntity.ok().build());
    }
}
