package com.aremi.musicstreamingservice.controller;

import com.aremi.common.logging.annotation.Monitor;
import com.aremi.musicstreamingservice.service.PreloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/private/preload")
@RequiredArgsConstructor
@Validated
@Monitor
public class PreloadController {

    private final PreloadService preloadService;

    @PostMapping("/activeSong")
    @PreAuthorize("hasRole('STREAMER')")
    public Mono<ResponseEntity<Void>> preloadActiveSong(@AuthenticationPrincipal Jwt principal) {
        String userId = principal.getSubject();
        return preloadService.preloadActiveSong(userId)
                .thenReturn(ResponseEntity.ok().build());
    }

/*    @PostMapping("/playlist")
    @PreAuthorize("hasRole('STREAMER')")
    public Mono<ResponseEntity<Void>> preloadUserPlaylist(@AuthenticationPrincipal Jwt principal) {
        String userId = principal.getSubject();
        return preloadService.preloadUserPlaylist(userId)
                .thenReturn(ResponseEntity.ok().build());
    }*/
}
