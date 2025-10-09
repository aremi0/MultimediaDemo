package com.aremi.musicstreamingservice.dto;

public record ActiveSongMetadata(String songId, String title, String artist, String genre, String filePath, Integer totalChunks) { }
