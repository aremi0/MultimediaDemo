package com.aremi.musicstreamingservice.exception;

import lombok.Getter;

@Getter
public final class ChunkNotFoundException extends AudioException {

    private final int chunkIndex;

    public ChunkNotFoundException(int chunkIndex) {
        super("Chunk audio non trovato: index=" + chunkIndex);
        this.chunkIndex = chunkIndex;
    }

}
