# Spring Distributed Infrastructure with Eureka, API Gateway, Kafka Logging and Docker

## 🧭 Overview

Questa infrastruttura software è progettata per supportare un ecosistema di microservizi distribuiti basati su **Spring Boot**. Include:

- **Service Discovery** tramite Eureka Server
- **SSL/HTTPS** tramite un Reverse-Proxy posto come unico punto di ingresso alla subnet, per una gestione centralizzata di HTTPS
- **API Gateway** per l'instradamento centralizzato delle richieste, posto dopo il Reverse-Proxy e non esposto verso l'esterno
- **Bilanciamento dinamico del carico** tramite Spring Cloud Gateway ===> DEPRECATA, da sostituire in update futuri
- **Logging centralizzato** tramite Apache Kafka
- **Containerizzazione con Docker** per una gestione semplificata
- **Estendibilità** per aggiungere nuovi servizi Spring in futuro
- **OAuth2** per la protezione delle risorse (~~per adesso solamente in http~~ integrazione https in sviluppo)
- **Frontend** spartano, integrato con il sistema di autenticazione e che in update futuri integrerà funzionalità di streaming MP3 e PDF 

---

## 🧱 Componenti principali

### 1. Eureka Server
- Funziona come **registro dei servizi**
- Permette ai microservizi di registrarsi e scoprirsi tra loro
- Porta predefinita: `8761`

### 2. Reverse-Proxy HTTPS
- Unico punto di ingresso alla subnet e all'infrastruttura
- Centralizza la cifratura SSL dall'esterno, permettendo quindi una comunicazione intranet semplificata in HTTP
- Permette di applicare agevolmente filtri, controlli di sicurezza e limitazioni del traffico in entrata (DA INTEGRARE)
- Più informazioni [qui](./certs/https-readme.md)

### 2. API Gateway
- Basato su **Spring Cloud Gateway (WebFlux)** ===> DEPRECATO da poco, da sistemare
- Si registra su Eureka e instrada le richieste ai servizi downstream
- Supporta **Discovery Locator** per generare automaticamente le rotte
- Configurato come client `confidential` per l'authentication

### 3. Logging centralizzato con Kafka
- Il sistema include un **Kafka Producer Service** che si occuperà di interagire con il `Kafka Broker`, tutti i servizi che vogliono scrivere un messaggio si interfacciano con esso ===> DA IMPLEMENTARE
- Ogni servizio/microservizio può pubblicare i propri log su un topic dedicato (es. `user-service-log`, `order-service-log`) attraverso il **Kafka Producer Service**
- I log possono essere successivamente consumati da un'applicazione di monitoraggio o da un sistema di persistenza
- Le informazioni di Kafka sono visualizzabili tramite **Kafka UI** (browser) alla porta `8085`.

### 5. Servizio Spring "Demo"
- Configurato come resource server `bearer-only` per l'authorization

### 6. Server Keycloak per l'Authentication
- [Configurazione server Keycloak](./keycloak-readme.md)
- [Informazioni sul flusso](./integrazione-ouath2.md)
- Il sistema include un server **Keycloak** runnato in modalità PROD, non esposto fuori dalla subnet
- Si appoggia su un database `postgress` per il salvataggio delle configurazioni
- Per interazioni dall'esterno della sottorete è stato posto un server Reverse-Proxy come layer intermediario tra l'esterno ed il server keycloak. Vai [qui](./keycloak-readme.md#8-configurazione-e-creazione-del-frontend-client-con-integrazione-per-reverse-proxy) per informazioni su come interrogarlo

### 7. Interfaccia Frontend spartana per le interazioni con l'architettura
- Scritto in HTML e Javascript puri per fornire una navigazione più scorrevole rispetto all'uso di un http-client, non il massimo della sicurezza ma trascurabile per i fini formativi del progetto
- Anch'esso è un container non esposto direttamente all'esterno della subnet, ma nascosto dietro il Reverse-Proxy in HTTPS e accessibile su `https://localhost/multimedia`
- Si interfaccia con il server di Authentication Keycloak tramite Reverse-Proxy HTTPS per garantire la cifratura della comunicazione e la corretta gestione di CORS e forwarding
- Integrerà nei prossimi aggiornamenti i servizi forniti dai microservizi Spring presenti nell'architettura, tra cui streaming MP3 e PDF

---

## ⚖️ Bilanciamento dinamico con Spring Cloud Gateway

Spring Cloud Gateway utilizza **Spring Cloud LoadBalancer** per distribuire dinamicamente le richieste tra le istanze dei servizi registrati su Eureka

### Come funziona:
- Ogni servizio si registra su Eureka con un `serviceId`
- Il Gateway, con `discovery.locator.enabled=true`, rileva automaticamente i servizi
- Le richieste vengono indirizzate usando URI come: `lb://nome-servizio`
- Il LoadBalancer integrato distribuisce le richieste tra le istanze disponibili (round-robin di default)

---

## 🔍 Servizi di Monitoraggio

- **EurekaServer Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:8085
- **Keycloak Dashboard**: http://localhost:8888/admin/master/console/ da browser da esterno, attraverso uso del Reverse-Proxy
- **Frontend**: https://localhost/multimedia

---

## 📜 Swagger dei Servizi Spring

- **API Gateway**: https://localhost/api
- **Demo Service**: https://localhost/api/demo-service/

### Demo Service Swagger
```yaml
openapi: 3.0.1
info:
  title: Demo Service API
  description: API documentation for the Demo Service
  version: 1.0.0
paths:
  /v2/public/demo:
    get:
      summary: Get a string. No role neither accessToken are necessary to access this resource.
      description: Returns a demo response
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  message:
                    type: string
                    example: "This is a demo response"
  /v2/private/user:
    get:
      summary: Get a string. AccessToken and Keycloak->role 'USER' are necessary to access this resource.
      description: Returns a demo response
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  message:
                    type: string
                    example: "This is a demo response"

```
