package com.aremi.musicstreamingservice.controller;

import com.aremi.common.logging.annotation.Monitor;
import com.aremi.musicstreamingservice.service.StreamingService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/private/stream")
@RequiredArgsConstructor
@Validated
@Monitor
public class StreamingController {

    private final StreamingService streamingService;

    @GetMapping("/chunk/{index}")
    @PreAuthorize("hasRole('STREAMER')")
    public Mono<ResponseEntity<byte[]>> getChunk(@AuthenticationPrincipal Jwt principal,
                                                 @PathVariable @Min(0) int index) {
        String userId = principal.getSubject();
        return streamingService.getChunkForUser(userId, index)
                .map(chunk -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(chunk.length)
                        .body(chunk));
    }
}
