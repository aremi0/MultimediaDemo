package com.aremi.demoservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Classe che gestisce gli Endpoint legati all'autenticazione
 */

@Slf4j
@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
public class DemoController {

    @GetMapping("/public/demo")
    public ResponseEntity<String> demo(HttpServletRequest request) {
        log.info("Richiesta arrivata");
        return ResponseEntity.ok("Questo è un endpoint pubblico.");
    }

    // Endpoint protetto, accessibile solo ad utenti autenticati con ruolo USER_ROLE
    @PreAuthorize("hasRole(T(com.aremi.demoservice.security.Roles).USER)")
    @GetMapping("/private/user")
    public ResponseEntity<String> user(HttpServletRequest request,
                                       @RequestHeader Map<String, String> headers,
                                       @AuthenticationPrincipal Jwt jwt,
                                       Authentication authentication) {
        headers.forEach((key, value) -> log.info("Header {}: {}", key, value));
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        authorities.forEach(auth -> log.info("Authority: {}", auth.getAuthority()));
        log.info("Richiesta arrivata: subject:{}, roles:{}", jwt.getSubject(), jwt.getClaim("realm_access"));
        //kafkaProducer.sendLog("demoController", "GET /private/user");
        return ResponseEntity.ok("Accesso consentito all'utente con ruolo user-role.");
    }
}
