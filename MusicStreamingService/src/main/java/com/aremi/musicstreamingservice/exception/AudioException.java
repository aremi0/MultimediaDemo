package com.aremi.musicstreamingservice.exception;

public sealed class AudioException extends RuntimeException
        permits ChunkNotFoundException, AudioReadException, AudioFormatException {

    public AudioException(String message) {
        super(message);
    }

    public AudioException(String message, Throwable cause) {
        super(message, cause);
    }
}
