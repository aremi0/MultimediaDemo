# === STAGE 1: build dell'immagine server ===
FROM quay.io/keycloak/keycloak:26.1.4 as builder

# Imposta eventuali opzioni (es. metrics, features)
ENV KC_DB=postgres
#    KC_METRICS_ENABLED=true \
#    KC_HEALTH_ENABLED=true

WORKDIR /opt/keycloak

# Copia i provider custom (se non ne hai, puoi saltare questa riga)
# COPY providers/ /opt/keycloak/providers/

# Esegui la build una volta sola in fase di image build
RUN /opt/keycloak/bin/kc.sh build

# === STAGE 2: immagine finale in runtime ===
FROM quay.io/keycloak/keycloak:26.1.4

WORKDIR /opt/keycloak

# Copia dal builder tutta la directory /opt/keycloak già compilata
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Copia eventuali certificati HTTPS già in immagine (opzionale)
# COPY certs/ /opt/keycloak/certs/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized"]
