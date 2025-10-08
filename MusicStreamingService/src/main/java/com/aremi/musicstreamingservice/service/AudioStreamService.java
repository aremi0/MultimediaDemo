package com.aremi.musicstreamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioStreamService {

    private final ReactiveRedisTemplate<String, byte[]> redisTemplate;

    public Mono<byte[]> getChunk(int index) {
        String key =
    }
}
