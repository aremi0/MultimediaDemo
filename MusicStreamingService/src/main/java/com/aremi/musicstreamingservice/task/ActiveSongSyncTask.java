package com.aremi.musicstreamingservice.task;

import com.aremi.musicstreamingservice.service.StreamingSessionService;
import com.aremi.musicstreamingservice.service.UserStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveSongSyncTask {

    private final StreamingSessionService streamingSessionService;
    private final UserStateService userStateService;

    /**
     * Lista degli utenti attivi da sincronizzare.
     * In produzione, questa dovrebbe essere recuperata da sessioni attive, token validi o Redis.
     */
    private List<String> getActiveUserIds() {
        // TODO: Integrare con session manager o token store
        return List.of(); // placeholder
    }

    /**
     * Task schedulato che sincronizza lo stato attuale da Redis verso MongoDB.
     * Esegue ogni 5 minuti.
     */
    //TODO: importare da properties la DDL di redis e di caffeine
    //TODO: lo scheduled job deve partire per ogni 5 minuti per ogni utente
    @Scheduled(fixedRate = 300_000) // ogni 5 minuti
    public void syncActiveSongs() {
        List<String> activeUsers = getActiveUserIds();

        if (activeUsers.isEmpty()) {
            log.info("⏳ Nessun utente attivo da sincronizzare.");
            return;
        }

        log.info("🔄 Inizio sincronizzazione activeSongId per {} utenti...", activeUsers.size());

        Flux.fromIterable(activeUsers)
                .flatMap(userId -> streamingSessionService.getActiveSongId(userId)
                        .flatMap(songId -> userStateService.updateActiveSong(userId, songId))
                        .doOnError(ex -> log.error("❌ Errore sincronizzazione utente: userId={}", userId, ex))
                )
                .doOnComplete(() -> log.info("✅ Sincronizzazione completata."))
                .subscribe();
    }
}
