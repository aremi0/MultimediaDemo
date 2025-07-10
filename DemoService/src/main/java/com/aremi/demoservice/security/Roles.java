package com.aremi.demoservice.security;

import lombok.Getter;

@Getter
public enum Roles {
    USER("USER");

    private final String nome;

    Roles(String nome) {
        this.nome = nome;
    }
}
