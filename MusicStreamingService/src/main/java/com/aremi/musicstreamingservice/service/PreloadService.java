package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.dto.ActiveSongMetadata;
import com.aremi.musicstreamingservice.io.AudioFileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
                          ReactiveRedisTemplate<String, byte[]> chunksRedisTemplate,
                          @Qualifier("metadataRedisTemplate") ReactiveRedisTemplate<String, String> metadataRedisTemplate,
                          StreamingSessionService streamingSessionService,
                          UserStateService userStateService) {
        super(audioFileReader, chunksRedisTemplate, metadataRedisTemplate);
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
    public Mono<ActiveSongMetadata> preloadActiveSong(String userId) {
        return streamingSessionService.getActiveSong(userId)
                .switchIfEmpty(
                        userStateService.findActiveSongByUserId(userId)
                                .flatMap(song -> streamingSessionService.setActiveSong(userId, song))
                )
                .flatMap(metadata -> {
                    int preloadLimit = Math.min(MAX_CHUNKS_TO_PRELOAD, metadata.totalChunks());
                    log.info("🚀 Precarico i primi {} chunk: userId={}, songId={}", preloadLimit, userId, metadata.songId());

                    return Flux.range(0, preloadLimit)
                            .concatMap(index -> getChunkFromRedisOrDisk(userId, metadata, index))
                            .then(Mono.just(metadata));
                })
                .doOnSuccess(v -> log.info("✅ Preload completato per userId={}", userId))
                .doOnError(ex -> log.error("❌ Errore durante preloadActiveSong: userId={}", userId, ex));
    }

    public Mono<Void> preloadNextChunks(String userId, ActiveSongMetadata info, int lastBufferedIndex) {
        int preloadStart = lastBufferedIndex + 1;
        int preloadEnd = Math.min(preloadStart + MAX_CHUNKS_TO_PRELOAD, info.totalChunks());
        log.info("🚀 Precarico i {} chunk successivi, startChunk={}, endChunk={}", MAX_CHUNKS_TO_PRELOAD, preloadStart, preloadEnd);

        return Flux.range(preloadStart, preloadEnd - preloadStart)
                .concatMap(index -> getChunkFromRedisOrDisk(userId, info, index)
                        .doOnNext(chunk -> log.info("📦 Precaricato chunk {}: userId={}, songId={}", index, userId, info.songId()))
                ).then();
    }

}

