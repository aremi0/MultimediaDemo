package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.exception.ChunkNotFoundException;
import com.aremi.musicstreamingservice.io.AudioFileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Servizio responsabile dello streaming dei chunk audio verso il frontend.
 * <p>
 * Estende {@link AbstractChunkService} per riutilizzare la logica comune
 * di recupero chunk da Redis o dal file system, ma si concentra sul caso
 * specifico dello streaming in tempo reale della canzone attiva (in riproduzione) di un utente.
 * </p>
 *
 * <h2>Responsabilità</h2>
 * - Recuperare le informazioni sulla canzone attiva dell'utente da Redis tramite {@link StreamingSessionService}.<br>
 * - Servire chunk audio richiesti dal frontend, cercandoli prima in Redis e in caso di mancanza leggendo dal disco.<br>
 * - Aggiornare la cache Redis con i chunk letti da disco per ottimizzare le richieste successive.<br>
 * - Restituire i dati binari al chiamante in modo reattivo.<br>
 *
 * <h2>Note</h2>
 * A differenza di {@link PreloadService}, questo servizio restituisce i byte dei chunk
 * al client, poiché il suo scopo è alimentare la riproduzione audio in tempo reale.
 *
 * @see AbstractChunkService
 * @see StreamingSessionService
 */

@Slf4j
@Service
public class StreamingService extends AbstractChunkService {

    private final StreamingSessionService streamingSessionService;
    private final PreloadService preloadService;

    public StreamingService(AudioFileReader audioFileReader,
                            ReactiveRedisTemplate<String, byte[]> chunksRedisTemplate,
                            @Qualifier("metadataRedisTemplate") ReactiveRedisTemplate<String, String> metadataRedisTemplate,
                            StreamingSessionService streamingSessionService,
                            PreloadService preloadService) {
        super(audioFileReader, chunksRedisTemplate, metadataRedisTemplate);
        this.streamingSessionService = streamingSessionService;
        this.preloadService = preloadService;
    }

    /**
     * Recupera un chunk audio della canzone attiva dell'utente e lo restituisce al frontend.
     * <p>
     * - Se il chunk è presente in Redis, viene restituito direttamente.<br>
     * - Se non è presente, viene letto dal file system e salvato in Redis.<br>
     * - Se il chunk non è disponibile né in Redis né su disco, viene sollevata
     *   una {@link ChunkNotFoundException}.<br>
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @param index  indice del chunk richiesto (>= 0)
     * @return un {@link Mono} contenente i byte del chunk richiesto,
     *         oppure errore se il chunk non è disponibile
     */
    public Mono<byte[]> getChunkForUser(String userId, int index) {
        return streamingSessionService.getActiveSong(userId)
                .flatMap(info -> getChunkFromRedisOrDisk(userId, info, index))
                .switchIfEmpty(Mono.error(new ChunkNotFoundException(index)))
                .doOnSuccess(chunk -> log.info("🎧 Chunk servito: userId={}, index={}, size={} bytes",
                        userId, index, chunk.length))
                .doOnError(ex -> log.error("❌ Errore durante getChunkForUser: userId={}, index={}", userId, index, ex));

        //TODO: fargli chiamare PreloadService.preloadNextChunk(userId, info, index)
    }

    /**
     * Stream continuo dei chunk audio della canzone attiva dell'utente.
     * <p>
     * Recupera i metadati della canzone attiva, legge il numero totale di chunk
     * da Redis (con fallback su disco), e costruisce un {@code Flux<byte[]>}
     * che serve ciascun chunk in sequenza, precaricando in background i successivi.
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @return flusso reattivo di chunk audio
     */
    public Flux<byte[]> streamActiveSong(String userId) {
        return streamingSessionService.getActiveSong(userId)
                .flatMapMany(info ->
                        getTotalChunksFromRedisOrDisk(info)
                                .map(Integer::parseInt)
                                .flatMapMany(totalChunks -> {
                                    if (totalChunks <= 0) {
                                        log.warn("⚠️ Nessun chunk disponibile per songId={}, filePath={}", info.songId(), info.filePath());
                                        return Flux.empty();
                                    }

                                    log.info("🎧 Streaming songId={} per userId={}, chunks={}", info.songId(), userId, totalChunks);

                                    return Flux.range(0, totalChunks)
                                            .buffer(MAX_CHUNKS_TO_PRELOAD)
                                            .concatMap(bufferedIndexes -> {
                                                Flux<byte[]> servingFlux = Flux.fromIterable(bufferedIndexes)
                                                        .concatMap(index -> getChunkFromRedisOrDisk(userId, info, index)
                                                                .onErrorResume(ex -> {
                                                                    log.error("❌ Errore nel chunk {}: userId={}, songId={}", index, userId, info.songId(), ex);
                                                                    return Mono.empty();
                                                                })
                                                        );

                                                int preloadStart = bufferedIndexes.getLast();
                                                Mono<Void> preload = preloadService.preloadNextChunks(userId, info, preloadStart);

                                                return preload.thenMany(servingFlux);
                                            });

                                })
                );
    }

}

