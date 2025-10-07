package com.aremi.demoservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Classe che si occupa di mappare i ruoli del Realm di Keycloak in ruoli compatibili con Spring
 *
 * Trasforma i ruoli di Keycloak (es: USER o admin-5) in GrantetAuthorities (ROLE_USER o ROLE_ADMIN_5)
 */

@Slf4j
public class KeyCloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if(Objects.isNull(realmAccess) || !realmAccess.containsKey("roles")) {
            return List.of();
        }

        log.info(realmAccess.toString());
        log.info("Token roles: " + realmAccess);

        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().replace("-", "_")))
                .collect(Collectors.toList());
    }
}
