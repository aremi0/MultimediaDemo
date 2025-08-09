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
    - `cert.pem`: certificato autofirmato
    - `key.pem`: chiave privata

#### 📜 Script usato: `generate-cert.sh`
```bash
#!/bin/bash

# Crea la cartella certs
mkdir -p certs

# Crea file di configurazione OpenSSL con SAN
cat > certs/openssl.cnf <<EOF
[req]
default_bits       = 4096
distinguished_name = req_distinguished_name
req_extensions     = req_ext
x509_extensions    = v3_ca
prompt             = no

[req_distinguished_name]
CN = localhost

[req_ext]
subjectAltName = @alt_names

[v3_ca]
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
EOF

# Genera certificato e chiave
openssl req -x509 -nodes -days 365 \
  -newkey rsa:4096 \
  -keyout certs/key.pem \
  -out certs/cert.pem \
  -config certs/openssl.cnf \
  -extensions req_ext

# Rimuove il file temporaneo
rm certs/openssl.cnf

echo "✅ Certificato generato in ./certs/"
```

#### 🔍 Comandi di verifica:
```bash
openssl x509 -in certs/cert.pem -text -noout      # Legge il certificato
openssl rsa -in certs/key.pem -check -noout       # Verifica la chiave privata
```

---

## 🔐 Strategia adottata

### Reverse Proxy HTTPS con Nginx
- Introduzione di un container `nginx-https` che:
    - Espone la porta `443`
    - Usa certificati autofirmati (`cert.pem`, `key.pem`)
    - Instrada le richieste verso i servizi interni in HTTP
    - Ingloba il container `keycloak-proxy`

---

### 🔧 Configurazione Nginx

#### Location definite:
Consulta il file di [configurazione](./nginx/https.conf)

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