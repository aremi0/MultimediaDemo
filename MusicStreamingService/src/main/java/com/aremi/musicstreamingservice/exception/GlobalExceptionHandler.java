package com.aremi.musicstreamingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AudioReadException.class)
    public Mono<ResponseEntity<String>> handleAudioRead(AudioReadException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMessage()));
    }

    @ExceptionHandler(ChunkNotFoundException.class)
    public Mono<ResponseEntity<String>> handleChunkNotFound(ChunkNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Chunk non disponibile: index=" + ex.getChunkIndex()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<String>> handleGeneric(Exception ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Errore interno: " + ex.getMessage()));
    }
}
