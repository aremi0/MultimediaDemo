package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreloadService {

    private final SongRepository songRepository;
    private final ReactiveRedisTemplate<String, byte[]> redisTemplate;

    public Mono<Void> preloadForUser(String userId) {
        return songRepository.findTopByOrderByPlayCountDesc()
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Nessuna canzone trovata per pre-caching");
                    return Mono.empty();
                }))
                .flatMap(song -> {
                    Optional<byte[]> maybeChunk = readFirstChunk(song.getFilePath());

                    if(maybeChunk.isEmpty()) {
                        log.warn("Chunk non disponibile per file: {}", song.getFilePath());
                        return Mono.empty();
                    }

                    String key = "preload:%s:%s:%d".formatted(userId, song.getId(), 0);
                    return redisTemplate.opsForValue()
                            .set(key, maybeChunk.get(), Duration.ofMinutes(5))
                            .doOnSuccess(ok -> log.info("Chunk salvato in Redis con chiave {}", key));
                })
                .then();
    }

    private Optional<byte[]> readFirstChunk(String filePath) {
        int chunkSize = 256 * 1024; // 256 KB

        try(InputStream is = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead = is.read(buffer);

            if(bytesRead <= 0) {
                log.warn("Nessun byte letto dal file: {}", filePath);
                return Optional.empty();
            }

            byte[] chunk = bytesRead < chunkSize ? Arrays.copyOf(buffer, bytesRead) : buffer;
            return Optional.of(chunk);
        } catch (IOException ex) {
            log.error("Errore durante la lettura del primo chunk del file: {}", filePath, ex);
            return Optional.empty();
        }
    }
}
