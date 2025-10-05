# Accesso a Keycloak via NGINX Reverse Proxy

---

## 🎯 Obiettivo

In un'infrastruttura Docker isolata (subnet), Keycloak è esposto solo ai container interni tramite il nome DNS `http://keycloak:8080`.  
Tuttavia, per scopi di sviluppo, test o autenticazione manuale via browser o Insomnia, si rende necessario **accedere a Keycloak anche dalla macchina host** tramite `http://localhost:8081`).

---

## 🧩 Problema

Spring Security si aspetta che l'issuer URL (`ISS`) nei token corrisponda all'URL con cui interagiscono, ma:

- **Internamente**, i container usano `http://keycloak:8080`
- **Esternamente**, la tua macchina host non può usare `http://keycloak:8080` perché `keycloak` non è risolvibile fuori dalla subnet

Se si prova ad accedere da fuori usando `http://localhost:8080`, la validazione fallisce oppure il browser non raggiunge Keycloak.

---

## ✅ Soluzione: NGINX come Reverse Proxy

Utilizziamo un container NGINX come reverse proxy che:

- **espone la porta 8081 all'esterno (host)**
- **reindirizza il traffico verso il servizio interno Keycloak (`keycloak:8080`)**

In questo modo:

- Il browser, Insomnia o strumenti esterni possono accedere a `http://localhost:8081`
- La configurazione interna di Keycloak può rimanere coerente con l'issuer `http://keycloak:8080`
- Spring e gli altri container continuano a lavorare con il DNS interno `keycloak`

---

## 🛠 Architettura
```

\[Host/Browser] --> [http://localhost:8081](http://localhost:8081)
↓
\[NGINX reverse proxy container]
↓
[http://keycloak:8080](http://keycloak:8080) (subnet interna)

````

## 📝 Vantaggi

- 🔒 Nessuna modifica dell'`issuer` nei token
- 🧩 Nessun conflitto tra DNS esterno/interno
- 🧪 Possibilità di fare test da fuori (browser, curl, Postman, ecc.)
- 🐳 Si mantengono le best practice Docker: servizi separati, nome DNS interni per discovery

