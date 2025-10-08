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

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChunkService {

    protected static final Duration CHUNK_TTL = Duration.ofMinutes(5);

    protected final AudioFileReader audioFileReader;
    protected final ReactiveRedisTemplate<String, byte[]> redisTemplate;

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

    // Aggiorna TTL o Aggiunge se non esisteva
    private void asyncCache(String key, byte[] chunk) {
        Mono.fromRunnable(() -> {
            redisTemplate.opsForValue().set(key, chunk, CHUNK_TTL).subscribe();
            log.info("🧠 Chunk salvato su Redis: key={}, size={} bytes", key, chunk.length);
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    protected String buildChunkKey(String userId, String songId, int index) {
        return "preload:%s:%s:%d".formatted(userId, songId, index);
    }
}
