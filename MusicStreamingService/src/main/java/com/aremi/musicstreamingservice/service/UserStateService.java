package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.model.Song;
import com.aremi.musicstreamingservice.repository.SongRepository;
import com.aremi.musicstreamingservice.repository.UserStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserStateService {

    private final UserStateRepository userStateRepository;
    private final SongRepository songRepository;

    /**
     * Recupera la activeSong di un utente da Mongo (UserState + Song).
     */
    public Mono<Song> findActiveSongByUserId(String userId) {
        return userStateRepository.findById(userId)
                .flatMap(state -> {
                    if (state.getActiveSongId() == null) {
                        log.warn("⚠️ Nessuna activeSongId nello stato utente: userId={}", userId);
                        return Mono.empty();
                    }
                    return songRepository.findById(state.getActiveSongId());
                })
                .doOnNext(song -> log.info("🎯 ActiveSong recuperata da Mongo: userId={}, songId={}", userId, song.getId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("❌ Nessuna activeSong trovata in Mongo per userId={}", userId);
                    return Mono.empty();
                }));
    }
}
