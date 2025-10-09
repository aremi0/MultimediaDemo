package com.aremi.musicstreamingservice.io;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Componente responsabile della lettura dei file audio dal file system.
 * <p>
 * Fornisce un'API reattiva per leggere i file a chunk di dimensione fissa,
 * utile per lo streaming progressivo e la gestione della cache distribuita.
 * </p>
 *
 * <h2>Dettagli implementativi</h2>
 * - La dimensione di ciascun chunk è fissata a {@code 256 KB}.<br>
 * - La lettura avviene tramite {@link RandomAccessFile}, posizionandosi
 *   all'offset calcolato in base all'indice del chunk.<br>
 * - In caso di errore di I/O, viene loggato l'errore e restituito un {@link Mono#empty()}.
 *
 * <h2>Thread-safety</h2>
 * Ogni chiamata apre e chiude un nuovo {@link RandomAccessFile}, quindi
 * la classe è stateless e sicura da usare in contesti concorrenti.
 */

@Slf4j
@Component
public class AudioFileReader {

    /** Dimensione fissa di ciascun chunk (256 KB). */
    private static final int CHUNK_SIZE = 256 * 1024; // 256 KB

    /**
     * Legge un chunk del file audio a partire dall'indice specificato.
     * <p>
     * L'indice 0 corrisponde ai primi {@code CHUNK_SIZE} byte del file,
     * l'indice 1 ai successivi, e così via.
     * </p>
     *
     * @param filePath percorso assoluto del file audio sul file system
     * @param index    indice del chunk da leggere (>= 0)
     * @return un {@link Mono} contenente i byte del chunk letto,
     *         oppure {@link Mono#empty()} se si verifica un errore o se non ci sono più dati
     */
    public Mono<byte[]> readChunk(String filePath, int index) {
        long offset = (long) index * CHUNK_SIZE;

        return Mono.fromCallable(() -> {
            try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
                file.seek(offset);
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead = file.read(buffer);

                if(bytesRead <= 0) return null;
                return bytesRead < CHUNK_SIZE ? Arrays.copyOf(buffer, bytesRead) : buffer;
            }})
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(ex -> {
                log.error("Errore lettura chunk {} da file {}", index, filePath, ex);
                return Mono.empty();
        });
    }

    /**
     * Calcola il numero totale di chunk necessari per rappresentare un file audio.
     * <p>
     * La dimensione di ciascun chunk è fissata a {@link #CHUNK_SIZE}. Il metodo
     * legge la dimensione totale del file e la divide per la dimensione del chunk,
     * arrotondando per eccesso in modo da includere eventuali byte residui.
     * </p>
     *
     * <h2>Note</h2>
     * - In caso di errore di lettura del file (es. file non esistente o accesso negato),
     *   viene loggato l'errore e restituito {@code 0}.<br>
     * - Questo metodo è pensato per essere usato in fase di streaming o preload,
     *   quando si vuole conoscere in anticipo il numero di richieste necessarie.
     *
     * @param filePath percorso assoluto del file audio sul file system
     * @return numero totale di chunk, oppure {@code 0} in caso di errore
     */
    public Mono<Integer> calculateTotalChunks(String filePath) {
        return Mono.fromCallable(() -> {
            long fileSize = Files.size(Paths.get(filePath));
            return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("❌ Errore nel calcolo dei chunk: filePath={}", filePath, e);
            return Mono.just(0); // oppure Mono.error(new CustomException(...))
        });
    }
}
