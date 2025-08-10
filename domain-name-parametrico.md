## 🧭 Capitolo 1 – Obiettivo

L’obiettivo è **parametrizzare il `DOMAIN_NAME`** in modo centralizzato per tutta l'architettura, consentendo di modificare il dominio agendo
sul minor numero di elementi:

1. La variabile `ENTRYPOINT_DOMAIN_NAME` nel file `.env`
2. Il [client](./keycloak-readme.md#8-configurazione-e-creazione-del-frontend-client-con-integrazione-per-reverse-proxy) `frontend-client` nella console di amministrazione di Keycloak

Questa scelta semplifica la gestione del dominio in tutti i componenti, riducendo la duplicazione e il rischio di errori

In parallelo, è stato adottato un insieme di misure per **garantire la sicurezza dei container**, limitando la superficie d’attacco, proteggendo il filesystem e impedendo modifiche persistenti o accessi non autorizzati

---

## ⚙️ Capitolo 2 – Parametrizzazione Dominio

### 📡 2.1 - Parametrizzazione nei container NGINX

Nei container *NGINX* `multimedia-entrypoint` e `frontend`, la parametrizzazione è implementata come segue:

- La variabile `DOMAIN_NAME` viene interpolata dinamicamente tramite script bash `entrypoint.sh` che sovrascrive file necessari
attraverso l'uso del tool `envsubst` come segue: `> envsubst '${DOMAIN_NAME}' < template > target`
- I template sono montati in sola lettura (`:ro`) e trasformati in file temporanei in RAM

Questa configurazione garantisce che ogni componente sia automaticamente aggiornato al cambio di dominio, senza necessità di modifiche manuali nei file statici

---

## 🔐 Capitolo 3 – Sicurezza

### 🛡️ 3.1 - Misure nei container NGINX

Per garantire la robustezza dell’architettura, sono state adottate le seguenti misure comuni:

- **Filesystem in sola lettura** con `read_only: true`, impedendo scritture sul filesystem base
- **Montaggi selettivi e protetti** i file critici sono montati in modalità `:ro` (es. gli script `entrypoint.sh`)
- **Scrittura volatile tramite `tmpfs`** i file/directory che necessitano scrittura a runtime per la sostituzione della variabile d'ambiente `${DOMAIN_NAME}` sono montati in `:rw` in RAM con la direttiva *Docker*:
  ```yaml
  tmpfs:
    - /var/cache/nginx:rw,size=20m
    - /etc/nginx/conf.d:rw,size=1m
    - /usr/share/nginx/html:rw,size=2m
  ```
  Questo garantisce che ogni modifica venga persa al riavvio del container.

---

## 🧩 Capitolo 4 – Considerazioni finali

Questo schema:

- Minimizza la superficie d’attacco
- Protegge da modifiche runtime
- Isola i file dinamici in RAM
- È compatibile con ambienti CI/CD e orchestratori

Inoltre, è stata fatta una **considerazione importante**:  
Se i container non sono esposti all’esterno (es. nessuna porta pubblica aperta), **non è possibile accedere alla shell**
per eseguire comandi come `nginx -s reload`. E anche nel caso in cui un attaccante riuscisse ad accedere,
**le modifiche sarebbero scritte in RAM e quindi eliminate al prossimo riavvio**, rendendo inefficace qualsiasi tentativo di compromissione persistente.

---
