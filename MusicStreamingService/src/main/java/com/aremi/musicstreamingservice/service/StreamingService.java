package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.exception.ChunkNotFoundException;
import com.aremi.musicstreamingservice.io.AudioFileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class StreamingService extends AbstractChunkService {

    private final StreamingSessionService streamingSessionService;

    public StreamingService(AudioFileReader audioFileReader,
                            ReactiveRedisTemplate<String, byte[]> redisTemplate,
                            StreamingSessionService streamingSessionService) {
        super(audioFileReader, redisTemplate);
        this.streamingSessionService = streamingSessionService;
    }

    /**
     * Recupera un chunk per l'utente e lo restituisce al frontend.
     */
    public Mono<byte[]> getChunkForUser(String userId, int index) {
        return streamingSessionService.getActiveSong(userId)
                .flatMap(info -> getChunkFromRedisOrDisk(userId, info, index))
                .switchIfEmpty(Mono.error(new ChunkNotFoundException(index)))
                .doOnSuccess(chunk -> log.info("🎧 Chunk servito: userId={}, index={}, size={} bytes",
                        userId, index, chunk.length))
                .doOnError(ex -> log.error("❌ Errore durante getChunkForUser: userId={}, index={}", userId, index, ex));
    }
}

