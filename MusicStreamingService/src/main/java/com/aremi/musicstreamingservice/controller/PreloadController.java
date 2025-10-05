package com.aremi.musicstreamingservice.controller;

import com.aremi.musicstreamingservice.dto.LoginPreloadRequest;
import com.aremi.musicstreamingservice.service.PreloadService;
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
@RequestMapping("/v1/private/preload")
@RequiredArgsConstructor
@Validated
public class PreloadController {

    private final PreloadService preloadService;

    @PostMapping("/login")
    public Mono<ResponseEntity<Void>> loginPreload(@Valid @RequestBody LoginPreloadRequest request) {
        if(request == null || request.getUserId() == null || request.getUserId().isBlank()) {
            log.warn("Richiesta preload non valida: {}", request);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return preloadService.preloadForUser(request.getUserId())
                .thenReturn(ResponseEntity.ok().build());
    }
}
