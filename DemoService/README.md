Servizio DEMO per verificare il funzionamento della comunicazione interna.

---

## 🛠️ Creazione di un Keystore da Certificato Autofirmato

### 📁 File necessari

- `multimedia-entrypoint.crt` – Certificato autofirmato
- `key.pem` – Chiave privata associata

---

### 🔐 1. **Genera il keystore `.p12`**

Esegui questo comando:

```bash
openssl pkcs12 -export \
  -in multimedia-entrypoint.crt \
  -inkey key.pem \
  -out multimedia-keystore.p12 \
  -name multimedia-entrypoint
```

> Ti verrà chiesto di impostare una password per proteggere il keystore.

---

### ⚙️ 3. **Configurazione JVM o Spring Boot**

#### 🔧 JVM options:
```bash
-Djavax.net.ssl.trustStore=path/to/multimedia-keystore.p12
-Djavax.net.ssl.trustStorePassword=your_password
-Djavax.net.ssl.trustStoreType=PKCS12
```

---

### ✅ Verifica

Testa la connessione con `curl`:

```bash
curl -v https://auth-gateway/realms/multimedia-realm/.well-known/openid-configuration \
  --cacert multimedia-entrypoint.crt
```

Se ricevi una risposta JSON, il certificato è accettato.

---

Vuoi che ti prepari uno script `.sh` che esegue tutto questo in automatico? Potresti usarlo anche nel tuo CI/CD.