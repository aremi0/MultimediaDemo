package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.io.AudioFileReader;
import com.aremi.musicstreamingservice.model.ActiveSongInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servizio responsabile del precaricamento dei chunk audio in cache.
 * <p>
 * Estende {@link AbstractChunkService} per riutilizzare la logica comune
 * di recupero chunk da Redis o dal file system, ma si concentra sul caso
 * specifico del preload delle canzoni di un utente.
 * </p>
 *
 * <h2>Note</h2>
 * Questo servizio non restituisce i dati audio al chiamante: il suo unico scopo
 * è assicurarsi che i chunk potenzialmente richiedibili siano pronti in cache
 * per ridurre la latenza durante lo streaming.
 *
 * @see AbstractChunkService
 * @see StreamingSessionService
 * @see UserStateService
 */

@Slf4j
@Service
public class PreloadService extends AbstractChunkService {

    private final StreamingSessionService streamingSessionService;
    private final UserStateService userStateService;

    public PreloadService(AudioFileReader audioFileReader,
                          ReactiveRedisTemplate<String, byte[]> redisTemplate,
                          StreamingSessionService streamingSessionService,
                          UserStateService userStateService) {
        super(audioFileReader, redisTemplate);
        this.streamingSessionService = streamingSessionService;
        this.userStateService = userStateService;
    }

    /**
     * Precarica in cache il chunk iniziale (indice 0) della canzone attiva dell'utente.
     * <p>
     * - Se la canzone attiva è già presente in Redis, viene usata direttamente.<br>
     * - Se non è presente, viene recuperata da MongoDB tramite {@link UserStateService}
     *   e salvata in Redis tramite {@link StreamingSessionService}.<br>
     * - In entrambi i casi, il chunk 0 viene letto (da Redis o da disco) e salvato in cache.<br>
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @return un {@link Mono} che completa senza valore al termine del preload,
     *         oppure emette errore se il chunk non è disponibile
     */
    public Mono<Void> preloadActiveSong(String userId) {
        return streamingSessionService.getActiveSong(userId)
                .switchIfEmpty(
                        // fallback su Mongo se Redis non ha l'activeSong
                        userStateService.findActiveSongByUserId(userId)
                                .flatMap(song -> streamingSessionService.setActiveSong(userId, song.getId())
                                        .thenReturn(new ActiveSongInfo(song.getId(), song.getFilePath())))
                )
                // qui non restituiamo i byte, ma usiamo doOnNext per triggerare il caching
                .flatMap(info -> getChunkFromRedisOrDisk(userId, info, 0).then())
                .doOnSuccess(v -> log.info("✅ Preload completato per userId={}", userId))
                .doOnError(ex -> log.error("❌ Errore durante preloadActiveSong: userId={}", userId, ex));
    }
}

