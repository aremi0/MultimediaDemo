## Esempio di flusso per API protetta e role-based

```text
[NGINX]    ---> Rivece request su https://multimedia-entrypoint/multimedia ---> Rimbalza verso il Frontend
|
v
[Frontend] ---> Pressione pulsante LOGIN ---> Rimbalza verso il NGINX
|
v
[NGINX]    ---> Riceve request su https://multimedia-entrypoint/realms/multimedia-realms ---> Rimbalza verso Keycloak ---> Redirect del Frontend su Keycloak
|
v
[Keycloak] ---> Esegui la login ---> Successo! ---> Redirect di nuovo verso il Frontend con il token/cookie
|
v
[Frontend] ---> Autenticato! ---> Preme pulsante per API Privata ---> Rimbalza verso NGINX
|
v
[NGINX]    ---> Rivece request su https://multimedia-entrypoint/api/service-name ---> Rimbalza verso il Api-Gateway 
|
v
[ApiGateway] ---> Authorization: Bearer token ---> Routing con EurekaServer verso Service-Name
|
v
[Servizio Spring] ---> Check sulla validità del token
|
v
@PreAuthorize("hasRole('USER')") ---> Autorizzato!
|
v
[Response] ---> Risposta ritorna al Frontend
```

---

## 🛡️ Cosa significa "validare il token"?

Quando diciamo che **il Gateway o i servizi "validano" il token**, intendiamo:

1. **Verifica della firma** (es. con chiavi pubbliche esposte da Keycloak via `/.well-known/openid-configuration`)
2. **Controllo della scadenza (`exp`)**
3. **Controllo del `issuer` e `audience`** (cioè chi ha emesso il token e per chi è valido)

Spring fa questo in automatico se configuri:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://multimedia-entrypoint/realms/multimedia-realm
```

⚠️ **Importante**:
Anche se *entrambi* (Gateway e servizi) dovessero validano il token, è corretto: si tratta di **Resource Server indipendenti**. L’API Gateway può validare **prima**, per bloccare subito richieste invalide.

---

### ✅ **Opzione 1 (più semplice e diretta):**

**Non validare nulla nel Gateway**, ma semplicemente **inoltra il token così com'è** verso i microservizi.
Spring Cloud Gateway lo fa **in automatico** (se configurato) copiando l’`Authorization` header.

In questo caso, la validazione completa avviene **solo nei microservizi**.

---

### ✅ **Opzione 2 (più robusta e consigliata)**

**Il Gateway valida il token** **e** lo propaga. Così blocca token scaduti o non validi **prima** di farli arrivare ai servizi.

---
