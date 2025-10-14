package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.dto.ActiveSongMetadata;
import com.aremi.musicstreamingservice.io.AudioFileReader;
import com.aremi.musicstreamingservice.model.Song;
import com.aremi.musicstreamingservice.repository.SongRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Servizio responsabile della gestione della canzone attiva (activeSong) di un utente
 * all'interno della sessione di streaming.
 * <p>
 * Utilizza Redis come storage distribuito e veloce per mantenere lo stato corrente
 * della canzone attiva, serializzando e deserializzando le informazioni tramite Jackson.
 * </p>
 *
 * <h2>Responsabilità</h2>
 * - Impostare la canzone attiva di un utente in Redis, con un TTL predefinito.<br>
 * - Recuperare la canzone attiva da Redis e deserializzarla in {@link ActiveSongMetadata}.<br>
 * - Garantire che i dati siano coerenti con il repository delle canzoni ({@link SongRepository}).<br>
 *
 * <h2>Note</h2>
 * Questo servizio non gestisce direttamente i chunk audio, ma solo i metadati
 * della canzone attiva (songId e filePath), che vengono poi utilizzati da
 * {@link PreloadService} e {@link StreamingService}.
 *
 * @see ActiveSongMetadata
 * @see SongRepository
 * @see PreloadService
 * @see StreamingService
 */

@Slf4j
@Service
public class StreamingSessionService {

    public StreamingSessionService(@Qualifier("activeSongRedisTemplate") ReactiveRedisTemplate<String, String> activeSongRedisTemplate,
                                   @Qualifier("metadataRedisTemplate") ReactiveRedisTemplate<String, String> metadataRedisTemplate,
                                   ObjectMapper objectMapper, AudioFileReader audioFileReader) {
        this.activeSongRedisTemplate = activeSongRedisTemplate;
        this.metadataRedisTemplate = metadataRedisTemplate;
        this.objectMapper = objectMapper;
        this.audioFileReader = audioFileReader;
    }

    private static final Duration ACTIVE_SONG_TTL = Duration.ofMinutes(10);
    private static final Duration METADATA_SONG_TTL = Duration.ofMinutes(10);

    private final AudioFileReader audioFileReader;
    private final ReactiveRedisTemplate<String, String> metadataRedisTemplate;
    private final ReactiveRedisTemplate<String, String> activeSongRedisTemplate;
    private final ObjectMapper objectMapper; // Jackson

    /**
     * Imposta la canzone attiva per l'utente in Redis.
     * <p>
     * Serializza un {@link ActiveSongMetadata} in JSON e lo salva in Redis con un TTL.
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @param song document Mongo della canzone da impostare come attiva
     * @return un {@link Mono} che completa al termine dell'operazione,
     *         oppure emette errore in caso di problemi di serializzazione o salvataggio
     */
    public Mono<ActiveSongMetadata> setActiveSong(String userId, Song song) {
        if (song == null) {
            return Mono.error(new IllegalArgumentException("La canzone non può essere null"));
        }

        String activeSongMetadataKey = buildActiveSongKey(userId);
        String songMetadataKey = buildSongMetadataKey(song.getId());

        return audioFileReader.calculateTotalChunks(song.getFilePath())
                .map(totalChunks -> new ActiveSongMetadata(
                        song.getId(),
                        song.getTitle(),
                        song.getArtist(),
                        song.getGenre(),
                        song.getFilePath(),
                        totalChunks
                ))
                .flatMap(metadata ->
                        Mono.fromCallable(() -> objectMapper.writeValueAsString(metadata))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(json -> {
                                    Mono<Boolean> saveUserState = activeSongRedisTemplate.opsForValue()
                                            .set(activeSongMetadataKey, json, ACTIVE_SONG_TTL)
                                            .doOnSuccess(ok -> log.info("🧠 activeSong salvata: userId={}, key={}", userId, activeSongMetadataKey))
                                            .doOnError(ex -> log.error("❌ Errore salvataggio activeSong: userId={}, key={}", userId, activeSongMetadataKey, ex));

                                    Mono<Boolean> saveSongMetadata = metadataRedisTemplate.opsForValue()
                                            .set(songMetadataKey, json, METADATA_SONG_TTL)
                                            .doOnSuccess(ok -> log.info("📦 metadata canzone salvato: songId={}, key={}", song.getId(), songMetadataKey))
                                            .doOnError(ex -> log.error("❌ Errore salvataggio metadata canzone: songId={}, key={}", song.getId(), songMetadataKey, ex));

                                    return Mono.when(saveUserState, saveSongMetadata).thenReturn(metadata);
                                })
                );
    }

    /**
     * Recupera la canzone attiva di un utente da Redis.
     * <p>
     * Legge il valore JSON dalla cache, lo deserializza in {@link ActiveSongMetadata}
     * e lo restituisce. In caso di errore di deserializzazione o assenza del valore,
     * restituisce un {@link Mono#empty()}.
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @return un {@link Mono} contenente {@link ActiveSongMetadata} se presente,
     *         oppure vuoto se non trovato o in caso di errore di parsing
     */
    public Mono<ActiveSongMetadata> getActiveSong(String userId) {
        String key = buildActiveSongKey(userId);

        return activeSongRedisTemplate.opsForValue()
                .get(key)
                .flatMap(json ->
                        Mono.fromCallable(() -> objectMapper.readValue(json, ActiveSongMetadata.class))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnNext(metadata -> log.info("📥 activeSong recuperata da Redis: userId={}, activeSongMetadata={}", userId, metadata))
                                .onErrorResume(e -> {
                                    log.error("❌ Errore deserializzazione ActiveSongInfo: userId={}, key={}", userId, key, e);
                                    return Mono.empty();
                                })
                )
                .doOnError(ex -> log.error("❌ Errore recupero activeSong: userId={}, key={}", userId, key, ex));

        //TODO aggiungere fallback su MONGO
    }


    /**
     * Costruisce la chiave Redis per memorizzare la canzone attiva di un utente.
     *
     * @param userId identificativo univoco dell'utente
     * @return chiave Redis univoca per la canzone attiva
     */
    private String buildActiveSongKey(String userId) {
        return "activeSong:%s".formatted(userId);
    }

    private String buildSongMetadataKey(String songId) {
        return "metadata:%s".formatted(songId);
    }
}

