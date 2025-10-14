package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.dto.ActiveSongMetadata;
import com.aremi.musicstreamingservice.exception.ChunkNotFoundException;
import com.aremi.musicstreamingservice.io.AudioFileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Servizio astratto che fornisce la logica comune per la gestione dei chunk audio.
 * <p>
 * Implementa il pattern "cache-aside": i chunk vengono cercati prima in Redis,
 * e in caso di mancanza vengono letti dal file system tramite {@link AudioFileReader}
 * e successivamente salvati in cache.
 * </p>
 *
 * <h2>Responsabilità</h2>
 * - Definire la logica di recupero chunk da Redis o da disco.<br>
 * - Aggiornare o inserire i chunk in Redis con un TTL predefinito.<br>
 * - Fornire un metodo di utilità per costruire le chiavi Redis.<br>
 *
 * <h2>Estensione</h2>
 * Le classi concrete come {@code PreloadService} e {@code StreamingService}
 * estendono questa classe per specializzare il comportamento (precaricamento vs streaming).
 *
 * @see com.aremi.musicstreamingservice.service.PreloadService
 * @see com.aremi.musicstreamingservice.service.StreamingService
 */

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChunkService {

    protected static final int MAX_CHUNKS_TO_PRELOAD = 3;

    /** Tempo di vita (TTL) dei chunk salvati in Redis. */
    protected static final Duration CHUNK_TTL = Duration.ofMinutes(5);

    protected final AudioFileReader audioFileReader;
    protected final ReactiveRedisTemplate<String, byte[]> chunksRedisTemplate;
    private final ReactiveRedisTemplate<String, String> metadataRedisTemplate;

    /**
     * Recupera un chunk dalla cache Redis o, in caso di mancanza, dal file system.
     * <p>
     * - Se il chunk è presente in Redis, viene restituito e il TTL viene aggiornato.<br>
     * - Se il chunk non è presente, viene letto da disco e salvato in Redis.<br>
     * - Se il chunk non esiste nemmeno su disco, viene sollevata una {@link ChunkNotFoundException}.
     * </p>
     *
     * @param userId identificativo dell'utente
     * @param info   informazioni sulla canzone attiva (songId e filePath)
     * @param index  indice del chunk da recuperare (>= 0)
     * @return un {@link Mono} contenente i byte del chunk, oppure errore se non trovato
     */
    protected Mono<byte[]> getChunkFromRedisOrDisk(String userId, ActiveSongMetadata info, int index) {
        String chunkKey = buildChunkKey(userId, info.songId(), index);

        return chunksRedisTemplate.opsForValue().get(chunkKey)
                .flatMap(chunk -> {
                    log.info("✅ Redis HIT: userId={}, songId={}, index={}, size={} bytes", userId, info.songId(), index, chunk.length);

                    return cacheChunk(chunkKey, chunk);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("❌ Redis MISS: userId={}, songId={}, index={}", userId, info.songId(), index);
                    return audioFileReader.readChunk(info.filePath(), index)
                            .flatMap(chunk -> {
                                log.info("📂 Chunk letto da disco: userId={}, songId={}, index={}, size={} bytes",
                                        userId, info.songId(), index, chunk.length);

                                return cacheChunk(chunkKey, chunk);
                            })
                            .switchIfEmpty(Mono.error(new ChunkNotFoundException(index)));
                }));
    }

    //TODO da sistemare
/*    protected Mono<String> getTotalChunksFromRedisOrDisk(ActiveSongMetadata info) {
        String metadataKey = buildMetadataKey(info.songId());

        return metadataRedisTemplate.opsForValue().get(metadataKey)
                .flatMap(metadata -> {
                    log.info("✅ Redis HIT: songId={}, metadata={}", info.songId(), metadata);
                    asyncCache(metadataKey, metadata);
                    return Mono.just(metadata);
                })
                .switchIfEmpty(Mono.defer(() ->
                        audioFileReader.calculateTotalChunks(info.filePath())
                                .map(String::valueOf)
                                .doOnNext(totalChunks -> {
                                    log.info("📊 Calcolati totalChunks da disco: songId={}, totalChunks={}", info.songId(), totalChunks);
                                    asyncCache(metadataKey, totalChunks);
                                })
                ));
    }*/

    /**
     * Aggiorna il TTL di un chunk già presente in Redis o lo inserisce se non esisteva.
     * L'operazione viene eseguita in modo asincrono su un thread separato.
     *
     * @param key   chiave Redis del chunk
     * @param chunk contenuto del chunk da salvare
     */
    private Mono<byte[]> cacheChunk(String key, byte[] chunk) {
        return chunksRedisTemplate.opsForValue()
                .set(key, chunk, CHUNK_TTL)
                .doOnSuccess(ok -> log.info("🧠 Chunk salvato su Redis: key={}, size={} bytes", key, chunk.length))
                .doOnError(ex -> log.error("❌ Errore salvataggio chunk: key={}, size={} bytes", key, chunk.length, ex))
                .thenReturn(chunk);
    }


    /**
     * Aggiorna il TTL di un totalChunks già presente in Redis o lo inserisce se non esisteva.
     * L'operazione viene eseguita in modo asincrono su un thread separato.
     *
     * @param key   chiave Redis del totalChunks
     * @param totalChunks contenuto del totalChunks da salvare
     */
    private void asyncCache(String key, String totalChunks) {
        Mono.fromRunnable(() -> {
            metadataRedisTemplate.opsForValue().set(key, totalChunks, CHUNK_TTL).subscribe();
            log.info("🧠 Chunk salvato su Redis: key={}, value={} chunk", key, totalChunks);
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * Costruisce la chiave Redis per un chunk specifico.
     *
     * @param userId identificativo dell'utente
     * @param songId identificativo della canzone
     * @param index  indice del chunk
     * @return chiave Redis univoca per il chunk
     */
    protected String buildChunkKey(String userId, String songId, int index) {
        return "preload:%s:%s:%d".formatted(userId, songId, index);
    }

    /**
     * Costruisce la chiave Redis per i totalChunks di una canzone.
     *
     * @param songId identificativo della canzone
     * @return chiave Redis univoca per la canzone
     */
    protected String buildMetadataKey(String songId) {
        return "metadata:%s".formatted(songId);
    }
}
