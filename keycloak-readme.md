# Configurazione Keycloak con Docker e OAuth2

Questo documento spiega come configurare un server Keycloak, creare un realm, un client, un utente e testare l’autenticazione tramite OAuth2 password grant.

---

## 1. Accesso alla console di amministrazione
<s>
Switchare la ENV `KC_HOSTNAME=keycloak` per poter accedere alla console dal browser, dopo le modifiche riportare il valore a `keycloak`.  

Apri il browser e vai a:

`http://localhost:8081/`
</s>

**Con l'aggiornamento che ha introdotto il Reverse-Proxy è cambiata la modalità di accesso alla Keycloak Dashboard come segue**

Aprire il browser e recarsi all'url senza modificare nessuna variabile d'ambiente prima dell'avvio del container:
`http://localhost:8888/admin/master/console/`

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

## 8 Configurazione e creazione del frontend-client con integrazione per reverse-proxy

### Configurazioni iniziali
1. Modificare il `[command]` per il lancio del container *keycloak* da docker come segue: `command: [ "start", "--hostname", "http://localhost:8888", "--hostname-strict", "false", "--proxy-headers", "xforwarded" ]`  
Questi parametri abiliteranno il forwarding dell'hostnameV2 comunicando al server *keycloak* che si trova dietro un *proxy* (in questo caso esposto all'esterno alla porta `8888`)
2. Assicurarsi che il server `proxy` abbia un file di configurazione ben configurato per settare l'hostname della richiesta senza sovrascriverlo con il suo hostname interno (vedi [qui](/nginx/keycloak-reverseProxy.conf))
3. Assicurarsi che il server `frontend` rimbalzi le richieste di autenticazione verso il server `proxy` e che configuri correttamente il client per interagire con `keycloak` (vedi [qui](/frontend/src/script.js))

---

### Creazione frontend-client da Keycloak Dashboard (browser)
Accedi alla Dashboard di Keycloak tramite browser su `http://localhost:8888/admin/master/console/`

| Campo                           | Valore                    |
|---------------------------------|---------------------------|
| Client ID                       | `frontend-client`         |
| Access Type                     | `public`                  |
| Root URL                        | `http://localhost:4200`   |
| Home URL                        | `http://localhost:4200`   |
| Valid Redirect URIs             | `http://localhost:4200/*` |
| Valid Post Logout Redirect URIs | `http://localhost:4200`   |
| Web Origins                     | `http://localhost:4200`   |
| PKCE                            | `S256`                    |
| Standard Flow                   | `enabled`                 |
| Implicit Flow and others        | `disabled`                |

- **Access Type: public** => si imposta con le ultime due righe della tabella sopra  
- **PKCE: S256** => si imposta dopo aver creato il client, aprendolo e andando dal menu a tab su Advanced e poi scorrendo le voci fino a *Proof Key for Code Exchange Code Challenge Method* e selezionando dalla tendina `S256`

---

### Flusso di esecuzione: Frontend -> Reverse-Proxy - Keycloak

#### 🎯 Obiettivo
Consentire ad un'applicazione frontend HTML/JavaScript eseguita nel browser della macchina host di autenticarsi tramite
Keycloak, mantenendo Keycloak **non esposto direttamente** all'esterno della subnet Docker.
L'accesso avviene tramite un **reverse proxy Nginx**.

#### 🔁 Flusso di Autenticazione

1. **Accesso al frontend**: il browser accede a `http://localhost:4200`, che corrisponde al container `frontend` esposto verso l'esterno sulla porta `4200`.
2. **Login**: il frontend usa `keycloak-js` per iniziare il flusso di autenticazione. La richiesta viene inviata a `http://localhost:8888`,
cioè alla porta esterna del *reverse proxy*`.
3. **Reverse proxy**: Nginx riceve la richiesta sulla porta `8888` e la inoltra internamente a Keycloak sulla porta interna `8081`.
4. **Keycloak**: genera la pagina di login e la restituisce al browser tramite il proxy con una `redirect` su `http://localhost:8888`
(per questo devo esporre il *reverse proxy* verso l'esterno, perchè altrimenti né `http://keycloak:8081` né la porta interna del *reverse proxy*
sono raggiungibili dall'esterno della subnet e al tempo stesso *OAuth2* funziona proprio in questa maniera, ovvero una redirect verso
il servizio di Autenticazione che si occuperà egli stesso della sicurezza della comunicazione e dei dati)
5. **Autenticazione**: l’utente inserisce le credenziali. La richiesta viene inoltrata nuovamente al proxy e poi a Keycloak.
6. **Redirect**: dopo l’autenticazione, Keycloak redirige il browser verso `http://localhost:4200` con i token/cookie.
7. **Frontend**: riceve il token e lo visualizza, permettendo l’accesso alle API protette.

#### ✅ Motivazioni delle Scelte

- **Reverse proxy**: protegge Keycloak da accessi diretti esterni, migliorando la sicurezza.
- **Hostname v2**: garantisce redirect corretti e compatibilità con ambienti proxy.
- **PKCE + Authorization Code Flow**: migliora la sicurezza del flusso di autenticazione.
- **Configurazione client coerente**: assicura che Keycloak possa redirigere correttamente il browser e accettare richieste CORS.

---