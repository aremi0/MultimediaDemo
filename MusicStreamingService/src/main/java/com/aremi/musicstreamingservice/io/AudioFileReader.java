package com.aremi.musicstreamingservice.io;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.RandomAccessFile;
import java.util.Arrays;

@Slf4j
@Component
public class AudioFileReader {

    private static final int CHUNK_SIZE = 256 * 1024; // 256 KB

    public Mono<byte[]> readChunk(String filePath, int index) {
        long offset = (long) index * CHUNK_SIZE;

        return Mono.fromCallable(() -> {
            try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
                file.seek(offset);
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead = file.read(buffer);

                if(bytesRead <= 0) return null;
                return bytesRead < CHUNK_SIZE ? Arrays.copyOf(buffer, bytesRead) : buffer;
            }
        }).onErrorResume(ex -> {
            log.error("Errore lettura chunk {} da file {}", index, filePath, ex);
            return Mono.empty();
        });
    }
}
