package com.aremi.musicstreamingservice.service;

import com.aremi.musicstreamingservice.model.Song;
import com.aremi.musicstreamingservice.repository.SongRepository;
import com.aremi.musicstreamingservice.repository.UserStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servizio responsabile della gestione dello stato utente persistito in MongoDB.
 * <p>
 * Fornisce metodi per recuperare la canzone attiva (activeSong) di un utente,
 * combinando le informazioni salvate nel documento {@code UserState} con i
 * metadati completi della canzone presenti in {@link SongRepository}.
 * </p>
 *
 * <h2>Responsabilità</h2>
 * - Leggere lo stato utente dalla collezione {@code user_state}.<br>
 * - Recuperare l'identificativo della canzone attiva (activeSongId).<br>
 * - Risalire al documento {@link Song} corrispondente tramite {@link SongRepository}.<br>
 * - Fornire un fallback persistente quando Redis non contiene informazioni sulla canzone attiva.<br>
 *
 * <h2>Note</h2>
 * Questo servizio non gestisce direttamente la cache o lo streaming,
 * ma funge da sorgente di verità persistente per la canzone attiva
 * di un utente. Viene tipicamente usato da {@link PreloadService}
 * e {@link StreamingSessionService} come fallback.
 *
 * @see com.aremi.musicstreamingservice.model.UserState
 * @see SongRepository
 * @see UserStateRepository
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStateService {

    private final UserStateRepository userStateRepository;
    private final SongRepository songRepository;

    /**
     * Recupera la canzone attiva di un utente da MongoDB.
     * <p>
     * - Legge il documento {@code UserState} per ottenere l'activeSongId.<br>
     * - Se l'ID è presente, recupera il documento {@link Song} corrispondente.<br>
     * - Se non è presente o non esiste alcuna canzone associata, restituisce un {@link Mono#empty()}.<br>
     * </p>
     *
     * @param userId identificativo univoco dell'utente
     * @return un {@link Mono} contenente la {@link Song} attiva se trovata,
     *         oppure vuoto se non disponibile
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
