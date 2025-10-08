package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.io.AudioFileReader;
import com.aremi.musicstreamingservice.model.ActiveSongInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
     * Precarica il chunk 0 della activeSong dell'utente.
     * Non restituisce i dati, ma garantisce che siano in cache.
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

