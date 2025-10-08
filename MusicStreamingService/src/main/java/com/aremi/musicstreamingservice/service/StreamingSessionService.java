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

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingSessionService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SongRepository songRepository;
    private final ObjectMapper objectMapper; // Jackson

    private static final Duration ACTIVE_SONG_TTL = Duration.ofMinutes(10);

    /**
     * Imposta la canzone attiva su Redis per l'utente.
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
     * Recupera la canzone attiva da Redis per l'utente.
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

    private String buildActiveSongKey(String userId) {
        return "preload:%s:activeSong".formatted(userId);
    }
}

