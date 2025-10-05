package com.aremi.musicstreamingservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/v1/public/**").permitAll()
                        .anyExchange().authenticated()) // Accesso pubblico
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
