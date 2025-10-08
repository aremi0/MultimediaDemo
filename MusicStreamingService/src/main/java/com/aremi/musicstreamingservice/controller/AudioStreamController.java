package com.aremi.musicstreamingservice.controller;

import com.aremi.common.logging.annotation.Monitor;
import com.aremi.musicstreamingservice.service.AudioStreamService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/private/stream")
@RequiredArgsConstructor
@Validated
@Monitor
public class AudioStreamController {

    private final AudioStreamService audioStreamService;

    @PreAuthorize("hasRole('MUSIC')")
    @GetMapping("/chunk/{index}")
    public Mono<ResponseEntity<byte[]>> getAudioChunk(@PathVariable @Min(0) int index) {
        log.info("Richiesta di un chunk audio: {}", index);

    }
}
