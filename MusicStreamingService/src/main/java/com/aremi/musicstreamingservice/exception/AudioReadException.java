package com.aremi.musicstreamingservice.exception;

import lombok.Getter;

@Getter
public final class AudioReadException extends AudioException {

    public AudioReadException(String filePath, Throwable cause) {
        super("Errore lettura file audio: " + filePath, cause);
    }
}
