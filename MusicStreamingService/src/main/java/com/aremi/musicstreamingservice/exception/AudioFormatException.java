package com.aremi.musicstreamingservice.exception;

public final class AudioFormatException extends AudioException {

    public AudioFormatException(String filePath) {
        super("Formato audio non supportato: " + filePath);
    }
}
