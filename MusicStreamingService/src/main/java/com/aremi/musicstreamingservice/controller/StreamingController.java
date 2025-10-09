package com.aremi.musicstreamingservice.controller;

import com.aremi.common.logging.annotation.Monitor;
import com.aremi.musicstreamingservice.service.StreamingService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller responsabile dello streaming dei contenuti audio.
 * <p>
 * Espone endpoint protetti per servire in tempo reale i chunk audio
 * della canzone attiva dell'utente autenticato.
 * </p>
 *
 * <h2>Sicurezza</h2>
 * Gli endpoint sono accessibili solo agli utenti con ruolo {@code STREAMER}.
 *
 * <h2>Mapping</h2>
 * Base path: {@code /v1/private/stream}
 *
 * @see com.aremi.musicstreamingservice.service.StreamingService
 */

@Slf4j
@RestController
@RequestMapping("/v1/private")
@RequiredArgsConstructor
@Validated
@Monitor
public class StreamingController {

    private final StreamingService streamingService;

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasRole('STREAMER')")
    public Flux<byte[]> streamActiveSong(@AuthenticationPrincipal Jwt principal) {
        String userId = principal.getSubject();
        return streamingService.streamActiveSong(userId);
    }



    /**
     * Restituisce il chunk audio richiesto della canzone attiva dell'utente.
     * <p>
     * L'utente viene identificato tramite il token JWT, da cui si estrae lo userId.
     * Il chunk viene recuperato dal {@link StreamingService}, che gestisce la logica
     * di caching e fallback (Redis o disco).
     * </p>
     *
     * @param principal il token JWT dell'utente autenticato, da cui viene estratto lo userId
     * @param index     l'indice del chunk richiesto (deve essere >= 0)
     * @return {@link ResponseEntity} contenente i byte del chunk audio,
     * con header {@code Content-Type: application/octet-stream} e {@code Content-Length}
     */
    @GetMapping("/chunk/{index}")
    @PreAuthorize("hasRole('STREAMER')")
    public Mono<ResponseEntity<byte[]>> getChunk(@AuthenticationPrincipal Jwt principal,
                                                 @PathVariable @Min(0) int index) {
        String userId = principal.getSubject();
        return streamingService.getChunkForUser(userId, index)
                .map(chunk -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(chunk.length)
                        .body(chunk));
    }
}
