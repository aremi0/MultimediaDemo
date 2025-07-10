# Configurazione Keycloak con Docker e OAuth2

Questo documento spiega come configurare un server Keycloak, creare un realm, un client, un utente e testare l’autenticazione tramite OAuth2 password grant.

---

## 1. Accesso alla console di amministrazione
Switchare la ENV `KC_HOSTNAME=keycloak` per poter accedere alla console dal browser, dopo le modifiche riportare il valore a `keycloak`.  

Apri il browser e vai a:

```
http://localhost:8081/
```

Effettua il login con:

* Username: `admin`
* Password: `admin`

---

## 2. Creazione del Realm

1. Nel pannello a sinistra, clicca su **Master** in alto a sinistra e seleziona **Add realm**.
2. Dai un nome al realm, ad esempio `multimedia-realm`.
3. Salva.

---

## 3. Creazione del Realm-role (Ruolo) <opzionale>

1. Nel pannello a sinistra, clicca su **Realm roles**.
2. Dai un nome al ruolo, ad esempio `USER`.
3. Salva.

---

## 4. Creazione del Client 'confidential' per la creazione del token (login)

1. Dal menu a sinistra seleziona **Clients**.
2. Clicca su **Create**.
3. Inserisci:
    * Client ID: `gateway-service`
    * Client Protocol: `openid-connect`
    * Root URL: lascia vuoto o metti l'URL del gateway interno (es. `http://gateway-service:8080/`)
4. Salva.
5. Configura il client:

    * Access Type: `confidential` (**Client authentication**: `On`)
    * Authorization: `Off`
    * Abilita: `Standard Flow` (authorization code)
    * Abilita: `Direct Access Grants` (password grant)
    * Inserisci Valid Redirect URIs, ad esempio `*` (per test, in produzione specificare gli URL esatti)
6. Salva.
7. Vai nella tab **Credentials** e annota il `Secret` generato.

---

## 5. Creazione del Client 'bearer-only' per proteggere i servizi specifici

1. Dal menu a sinistra seleziona **Clients**.
2. Clicca su **Create**.
3. Inserisci:
   * Client ID: `demo-service`
   * Client Protocol: `openid-connect`
   * Root URL: lascia vuoto
4. Salva.
5. Configura il client:
   * Access Type: `Off` (**Client authentication**: `Off`)
   * Authorization: `Off`
   * Abilita: `Standard Flow` (authorization code)
   * Abilita: `Direct Access Grants` (password grant)
   * Inserisci Valid Redirect URIs, ad esempio `*` (per test, in produzione specificare gli URL esatti)
6. Salva.

---

## 6. Creazione Utente

1. Dal menu a sinistra seleziona **Users**.
2. Clicca su **Add user**.
3. Inserisci **TUTTI** i campi:
    * Username: `demo`
    * Email: `demo@example.com`
    * First Name: `Demo`
    * Last Name: `Demo`
4. Cancella eventuali **Required user action**
5. **Email verified**: `On`
5. Salva.
6. Vai nella tab **Credentials**.
7. Imposta la password (esempio: `demo`) e disabilita l’opzione **Temporary** (per evitare richieste di cambio password).
8. Salva.

### <opzionale> **Assegnazione ruolo**
1. Vai su Users → seleziona l’utente (es. demo).
2. Vai su Role Mappings.
3. Seleziona il ruolo `USER` da **Assign role** (seleziona filtro `Filter by realms roles`), clicca su **Assign**.

---

## 7. Testing dell'autenticazione

Usa il container `curl` per ottenere un token con grant type password:

```bash
curl -X POST "http://keycloak:8081/realms/multimedia-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=gateway-service" \
  -d "client_secret=IL_TUO_SECRET" \
  -d "username=demo" \
  -d "password=demo"
```

Se tutto funziona, otterrai un JSON con access token e refresh token.

---

## Note finali

* In produzione, sostituire le password e i segreti con valori sicuri.
* Limitare le `Valid Redirect URIs` solo ai domini e URL necessari.
* Proteggere l’accesso al container Keycloak.
* Integrare i token OAuth2 nell’applicazione (es. gateway, servizi).

---
