package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.exception.ChunkNotFoundException;
import com.aremi.musicstreamingservice.io.AudioFileReader;
import com.aremi.musicstreamingservice.model.ActiveSongInfo;
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

    /** Tempo di vita (TTL) dei chunk salvati in Redis. */
    protected static final Duration CHUNK_TTL = Duration.ofMinutes(5);

    protected final AudioFileReader audioFileReader;
    protected final ReactiveRedisTemplate<String, byte[]> redisTemplate;

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
    protected Mono<byte[]> getChunkFromRedisOrDisk(String userId, ActiveSongInfo info, int index) {
        String key = buildChunkKey(userId, info.songId(), index);

        return redisTemplate.opsForValue().get(key)
                .flatMap(chunk -> {
                    log.info("✅ Redis HIT: userId={}, songId={}, index={}, size={} bytes",
                            userId, info.songId(), index, chunk.length);
                    asyncCache(key, chunk);
                    return Mono.just(chunk);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("❌ Redis MISS: userId={}, songId={}, index={}", userId, info.songId(), index);
                    return audioFileReader.readChunk(info.filePath(), index)
                            .doOnNext(chunk -> {
                                log.info("📂 Chunk letto da disco: userId={}, songId={}, index={}, size={} bytes",
                                        userId, info.songId(), index, chunk.length);
                                asyncCache(key, chunk);
                            })
                            .switchIfEmpty(Mono.error(new ChunkNotFoundException(index)));
                }));
    }

    /**
     * Aggiorna il TTL di un chunk già presente in Redis o lo inserisce se non esisteva.
     * L'operazione viene eseguita in modo asincrono su un thread separato.
     *
     * @param key   chiave Redis del chunk
     * @param chunk contenuto del chunk da salvare
     */
    private void asyncCache(String key, byte[] chunk) {
        Mono.fromRunnable(() -> {
            redisTemplate.opsForValue().set(key, chunk, CHUNK_TTL).subscribe();
            log.info("🧠 Chunk salvato su Redis: key={}, size={} bytes", key, chunk.length);
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
}
