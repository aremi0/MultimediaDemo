package com.aremi.musicstreamingservice.conf.security;

public enum Roles {
    MUSIC("MUSIC"),;

    private final String nome;

    Roles(String nome) {
        this.nome = nome;
    }
}
