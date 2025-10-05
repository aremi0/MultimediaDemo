package com.aremi.musicstreamingservice.conf.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe che si occupa di mappare i ruoli del Realm di Keycloak in ruoli compatibili con Spring
 *
 * Trasforma i ruoli di Keycloak (es: USER o admin-5) in GrantetAuthorities (ROLE_USER o ROLE_ADMIN_5)
 */

@Slf4j
public class KeycloakReactiveRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = Optional.ofNullable(realmAccess)
                .map(r -> (List<String>) r.get("roles"))
                .orElse(List.of());

        log.info("Token roles: {}", roles);

        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().replace("-", "_")))
                .collect(Collectors.toList());

        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }
}

