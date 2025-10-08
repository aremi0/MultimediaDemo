package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.model.ActiveSongInfo;
import com.aremi.musicstreamingservice.repository.SongRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
 * - Recuperare la canzone attiva da Redis e deserializzarla in {@link ActiveSongInfo}.<br>
 * - Garantire che i dati siano coerenti con il repository delle canzoni ({@link SongRepository}).<br>
 *
 * <h2>Note</h2>
 * Questo servizio non gestisce direttamente i chunk audio, ma solo i metadati
 * della canzone attiva (songId e filePath), che vengono poi utilizzati da
 * {@link PreloadService} e {@link StreamingService}.
 *
 * @see ActiveSongInfo
 * @see SongRepository
 * @see PreloadService
 * @see StreamingService
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingSessionService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SongRepository songRepository;
    private final ObjectMapper objectMapper; // Jackson

    private static final Duration ACTIVE_SONG_TTL = Duration.ofMinutes(10);

    /**
     * Imposta la canzone attiva per l'utente in Redis.
     * <p>
     * Recupera la canzone dal {@link SongRepository} per ottenere i metadati necessari,
     * serializza un {@link ActiveSongInfo} in JSON e lo salva in Redis con un TTL.
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @param songId identificativo della canzone da impostare come attiva
     * @return un {@link Mono} che completa al termine dell'operazione,
     *         oppure emette errore in caso di problemi di serializzazione o salvataggio
     */
    public Mono<Void> setActiveSong(String userId, String songId) {
        return songRepository.findById(songId)
                .flatMap(song -> {
                    String key = buildActiveSongKey(userId);
                    try {
                        String value = objectMapper.writeValueAsString(
                                new ActiveSongInfo(song.getId(), song.getFilePath())
                        );

                        return redisTemplate.opsForValue()
                                .set(key, value, ACTIVE_SONG_TTL)
                                .doOnSuccess(ok -> log.info("🎯 activeSong impostata su Redis: userId={}, songId={}, filePath={}",
                                        userId, song.getId(), song.getFilePath()))
                                .doOnError(ex -> log.error("❌ Errore impostazione activeSong: userId={}, songId={}", userId, songId, ex))
                                .then();

                    } catch (JsonProcessingException e) {
                        log.error("❌ Errore serializzazione ActiveSongInfo: userId={}, songId={}", userId, songId, e);
                        return Mono.error(e);
                    }
                });
    }

    /**
     * Recupera la canzone attiva di un utente da Redis.
     * <p>
     * Legge il valore JSON dalla cache, lo deserializza in {@link ActiveSongInfo}
     * e lo restituisce. In caso di errore di deserializzazione o assenza del valore,
     * restituisce un {@link Mono#empty()}.
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @return un {@link Mono} contenente {@link ActiveSongInfo} se presente,
     *         oppure vuoto se non trovato o in caso di errore di parsing
     */
    public Mono<ActiveSongInfo> getActiveSong(String userId) {
        String key = buildActiveSongKey(userId);
        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(json -> {
                    try {
                        ActiveSongInfo info = objectMapper.readValue(json, ActiveSongInfo.class);
                        log.info("📥 activeSong recuperata da Redis: userId={}, songId={}, filePath={}",
                                userId, info.songId(), info.filePath());
                        return Mono.just(info);
                    } catch (JsonProcessingException e) {
                        log.error("❌ Errore deserializzazione ActiveSongInfo: userId={}", userId, e);
                        return Mono.empty();
                    }
                })
                .doOnError(ex -> log.error("❌ Errore recupero activeSong: userId={}", userId, ex));
    }

    /**
     * Costruisce la chiave Redis per memorizzare la canzone attiva di un utente.
     *
     * @param userId identificativo univoco dell'utente
     * @return chiave Redis univoca per la canzone attiva
     */
    private String buildActiveSongKey(String userId) {
        return "%s:activeSong".formatted(userId);
    }
}

