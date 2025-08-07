# <u>Questi scenari NON includono l'aggiornamento che ha introdotto il Reverse-Proxy all'interno della subnet</u>

```text
[Insomnia] ---> (login Request verso Keycloak) ---> riceve access_token
|
v
[Insomnia] ---> (Request verso servizio Spring) ---> riceve 200
|
v
Chiama API:
[Authorization: Bearer token]
|
v
[ApiGateway]
|
v
[Microservizio Protetto]
|
v
@PreAuthorize("hasRole('USER')")
```

---

## ✅ Riepilogo dello scenario

1. Eseguire login con Insomnia o CURL. → riceve un `access_token`
   * `curl -X POST "http://keycloak:8081/realms/multimedia-realm/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=password" -d "client_id=gateway-service" -d "client_secret=wqh8MSqeERT2DbZLfthyCXC6Ew1iIq2I" -d "username=demo"  -d "password=demo"`
2. **Il client (frontend o Postman)** fa request verso microservizio con accessToken → riceve un 200
3. Chiama un endpoint del **Gateway** con l'header: `Authorization: Bearer <access_token>`
4. Il **Gateway**:
    * (opzionale) **valida il token** → verifica che sia autentico e non scaduto
    * **propaga il token** al microservizio corretto (nell'header `Authorization`)
5. Il **microservizio**:
    * **ri-valida il token**
    * legge ruoli e applica logica con `@PreAuthorize(...)` o simili

---

## 🛡️ Cosa significa "validare il token"?

Quando diciamo che **il Gateway o i servizi "validano" il token**, intendiamo:

1. **Verifica della firma** (es. con chiavi pubbliche esposte da Keycloak via `/.well-known/openid-configuration`)
2. **Controllo della scadenza (`exp`)**
3. **Controllo del `issuer` e `audience`** (cioè chi ha emesso il token e per chi è valido)

Spring fa questo in automatico se configuri:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8081/realms/multimedia-realm
```

⚠️ **Importante**:
Anche se *entrambi* (Gateway e servizi) validano il token, è corretto: si tratta di **Resource Server indipendenti**. L’API Gateway può validare **prima**, per bloccare subito richieste invalide.

---

---

### ✅ **Opzione 1 (più semplice e diretta):**

**Non validare nulla nel Gateway**, ma semplicemente **inoltra il token così com'è** verso i microservizi.
Spring Cloud Gateway lo fa **in automatico** (se configurato) copiando l’`Authorization` header.

In questo caso, la validazione completa avviene **solo nei microservizi**.

---

### ✅ **Opzione 2 (più robusta e consigliata)**

**Il Gateway valida il token** **e** lo propaga. Così blocca token scaduti o non validi **prima** di farli arrivare ai servizi.

---
