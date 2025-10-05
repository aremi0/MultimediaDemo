## 🧾 **Abilitazione HTTPS in ambiente di sviluppo**

### ✅ **Obiettivo**
Integrare e centralizzare la gestione del protocollo HTTPS rimuovendo l'esposizione verso l'esterno della subnet per i servizi
{`frontend`, `gatewayService`, `keycloak-proxy`} usando un **certificato autofirmato** (SVILUPPO).

---

### 1. **Generazione certificato autofirmato**

#### 📁 Struttura creata:

Puoi usare *wsl2* per usare il comando `>$ openssl`

- Cartella: `certs/`
- File generati:
    - `cert.pem`: certificato autofirmato usato dal *Reverse-Proxy* per l'ingresso nella rete
    - `key.pem`: chiave privata usato dal *Reverse-Proxy* per l'ingresso nella rete
    - `multimedia-entrypoint.crt`: certificato autofirmato usato dal Keystore dei servizi Spring
    - `multimedia-entrypoint.p12`: keystore usati dai servizi Spring

<big>Il Keystore in *.p12 è stato generato con il comando</big>
```bash
openssl pkcs12 -export   -in multimedia-entrypoint.crt   -inkey key.pem   -out multimedia-keystore.p12   -name multimedia-entrypoint
````

#### 🔧 JVM options:
```bash
-Djavax.net.ssl.trustStore=path/to/multimedia-keystore.p12
-Djavax.net.ssl.trustStorePassword=your_password
-Djavax.net.ssl.trustStoreType=PKCS12
```

#### 🔍 Comandi di verifica:
```bash
openssl x509 -in certs/cert.pem -text -noout                     # Legge il certificato
openssl rsa -in certs/key.pem -check -noout                      # Verifica la chiave privata
openssl x509 -in certs/multimedia-entrypoint.crt -text -noout    # Legge il certificato
```

---

## 🔐 Strategia adottata

### Reverse Proxy HTTPS con Nginx
- Introduzione di un container *NGINX* `multimedia-entrypoint` che:
    - Espone la porta `443`
    - Usa certificati autofirmati (`cert.pem`, `key.pem`)
    - Instrada le richieste verso i servizi interni in HTTP
    - Ingloba il container `keycloak-proxy`

---

### 🔧 Configurazione Nginx

#### Location definite:
Consulta il file di [configurazione](../nginx/https.conf)

| Percorso      | Destinazione interna   | Scopo                                         |
|---------------|------------------------|-----------------------------------------------|
| `/api/`       | `gateway-service:8080` | API usate dal frontend                        |
| `/multimedia` | `frontend:80`          | Interfaccia utente HTML/Javascript            |
| `/`           | `keycloak:8081`        | Accesso diretto alla admin Dashboard Keycloak |


### Note:
- Il `gatewayService` non espone più la porta 8080 all’esterno → diventa un **servizio interno**.
- Il `frontend` non espone più la porta 4200 → diventa anch’esso **interno**, servito da Nginx.
- Il `keycloak-proxy` può essere rimosso → Nginx instrada direttamente verso `keycloak`.
- Non è necessario configurare HTTPS nei singoli microservizi → Nginx gestisce la cifratura.

---

### 🧠 Chiarimenti architetturali

#### 🔹 Location `/`
- Inizialmente il browser contattava `keycloak-proxy` su porta 8888.
- Ora Nginx può contattare direttamente `keycloak:8081`, eliminando un livello di proxy.
- Non è possibile usare una location `/auth/` perchè porterebbe incompatibilità in quanto keycloak usa come root `/`.

#### 🔹 Location `/multimedia/`
- Serve i file statici del frontend.
- Le chiamate API del frontend possono essere su percorsi come `/dashboard/api/...`, che Nginx gestisce correttamente.

### 🔹 HTTPS nel frontend
- Il frontend non gestisce direttamente HTTPS.
- È Nginx che serve i file statici e le API su HTTPS, usando il certificato autofirmato.

---

## 🔜 Prossimi step
- Verificare il corretto instradamento dei path `/api/`, `/test/`, `/auth/`
- Eventuale aggiunta di:
    - Logging delle richieste
    - Caching delle risposte statiche
    - Redirect automatico da HTTP a HTTPS

---

## 🛠️ Guida alla Modifica del File `hosts`

### 📍 Cos'è il file `hosts`?

Il file `hosts` è un file di sistema che mappa nomi di dominio a indirizzi IP **localmente**, bypassando il DNS.  
Percorsi comuni:

- **Windows**: `C:\Windows\System32\drivers\etc\hosts`
- **Linux/macOS**: `/etc/hosts`

---

### ✏️ Come modificarlo

1. **Apri il file con privilegi di amministratore**:
  - Windows: usa *Blocco Note* come amministratore.
  - Linux/macOS: usa `sudo nano /etc/hosts`.

2. **Aggiungi una riga nel formato**:
   ```
   127.0.0.1   multimedia-entrypoint
   ```

3. **Salva e chiudi**.

---

### ❓ Perché modificarlo?

- **Sviluppo locale**: Simula un dominio (es. `multimedia-entrypoint`) puntandolo a `localhost`.
- **Test certificati SSL**: Alcuni certificati richiedono un nome DNS specifico (es. SAN).
- **Bypass DNS temporaneo**: Risolve problemi di risoluzione o propagazione DNS.
- **Blocco siti**: Può essere usato per reindirizzare domini indesiderati a `127.0.0.1`.

---

### ⚠️ Attenzione

- Le modifiche influenzano **solo la macchina locale**.
- Riavvia il browser o svuota la cache DNS se non vedi subito l’effetto:
  ```bash
  ipconfig /flushdns   # Windows
  sudo dscacheutil -flushcache   # macOS
  ```

---
