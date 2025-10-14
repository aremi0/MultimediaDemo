package com.aremi.musicstreamingservice.controller;

import com.aremi.common.logging.annotation.Monitor;
import com.aremi.musicstreamingservice.dto.ActiveSongMetadata;
import com.aremi.musicstreamingservice.service.PreloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller responsabile delle operazioni di preload dei contenuti audio.
 * <p>
 * Espone endpoint protetti per precaricare in cache i chunk delle canzoni
 * associate all'utente autenticato, in modo da ridurre la latenza durante lo streaming.
 * </p>
 *
 * <p>
 * Attualmente gestisce il preload della canzone attiva (activeSong) al momento della login.
 * </p>
 *
 * <h2>Sicurezza</h2>
 * Gli endpoint sono accessibili solo agli utenti con ruolo {@code STREAMER}.
 *
 * <h2>Mapping</h2>
 * Base path: {@code /v1/private/preload}
 *
 * @see com.aremi.musicstreamingservice.service.PreloadService
 */

@Slf4j
@RestController
@RequestMapping("/v1/private/preload")
@RequiredArgsConstructor
@Validated
@Monitor
public class PreloadController {

    private final PreloadService preloadService;

    /**
     * Precarica in cache il chunk iniziale della canzone attiva dell'utente autenticato.
     * <p>
     * Recupera l'identificativo dell'utente dal token JWT e delega al {@link PreloadService}
     * la logica di caching. Non restituisce i dati audio, ma solo un {@code 200 OK}
     * a conferma dell'avvenuta operazione.
     * </p>
     *
     * @param principal il token JWT dell'utente autenticato, da cui viene estratto lo userId
     * @return {@link ResponseEntity} con stato {@code 200 OK} se il preload è stato avviato correttamente
     */
    @GetMapping("/activeSong")
    @PreAuthorize("hasRole('STREAMER')")
    public Mono<ResponseEntity<ActiveSongMetadata>> preloadActiveSong(@AuthenticationPrincipal Jwt principal) {
        String userId = principal.getSubject();
        return preloadService.preloadActiveSong(userId)
                .map(ResponseEntity::ok);
    }

    /*
     * Endpoint futuro per precaricare l'intera playlist dell'utente.
     * Attualmente disabilitato.
     */
/*    @PostMapping("/playlist")
    @PreAuthorize("hasRole('STREAMER')")
    public Mono<ResponseEntity<Void>> preloadUserPlaylist(@AuthenticationPrincipal Jwt principal) {
        String userId = principal.getSubject();
        return preloadService.preloadUserPlaylist(userId)
                .thenReturn(ResponseEntity.ok().build());
    }*/
}
