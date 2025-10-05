package com.aremi.musicstreamingservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<String, byte[]> context = RedisSerializationContext
                .<String, byte[]>newSerializationContext(RedisSerializer.string())
                .value(RedisSerializer.byteArray())
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
