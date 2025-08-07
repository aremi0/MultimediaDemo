# Spring Distributed Infrastructure with Eureka, API Gateway, Kafka Logging and Docker

## 🧭 Overview

Questa infrastruttura software è progettata per supportare un ecosistema di microservizi distribuiti basati su **Spring Boot**. Include:

- **Service Discovery** tramite Eureka Server
- **API Gateway** per l'instradamento centralizzato delle richieste
- **Bilanciamento dinamico del carico** tramite Spring Cloud Gateway
- **Logging centralizzato** tramite Apache Kafka
- **Containerizzazione con Docker** per una gestione semplificata
- **Estendibilità** per aggiungere nuovi servizi Spring in futuro
- **OAuth2** per la protezione delle risorse (per adesso solamente in http)
- **Frontend** spartano integrato con il sistema di autenticazione e che in update futuri integrerà funzionalità di streaming MP3 e PDF 

---

## 🧱 Componenti principali

### 1. Eureka Server
- Funziona come **registro dei servizi**
- Permette ai microservizi di registrarsi e scoprirsi tra loro
- Porta predefinita: `8761`

### 2. API Gateway
- Basato su **Spring Cloud Gateway (WebFlux)**
- Si registra su Eureka e instrada le richieste ai servizi downstream
- Supporta **Discovery Locator** per generare automaticamente le rotte
- Porta predefinita: `8080`
- Configurato come client `confidential` per l'authentication

### 3. Logging centralizzato con Kafka
- Il sistema include un **Kafka broker** per raccogliere i log da tutti i servizi
- Il **Gateway** pubblica i log sul topic `gateway-log`
- Ogni microservizio può pubblicare i propri log su un topic dedicato (es. `user-service-log`, `order-service-log`)
- I log possono essere successivamente consumati da un'applicazione di monitoraggio o da un sistema di persistenza
- Le informazioni di Kafka sono visualizzabili tramite **Kafka UI** (browser) alla porta `8085`.
- Ogni servizio è containerizzato con un `Dockerfile`
- Gestione orchestrata tramite `docker-compose`

### 5. Servizio Spring "Demo"
- È presente un servizio Spring "Demo" che presenta un endpoint **GET** `lb://demo-service/api/demo`
- Configurato come resource server `bearer-only` per l'authorization

### 6. Server Keycloak per l'Authentication
- [Configurazione server Keycloak](./keycloak-readme.md)
- [Informazioni sul flusso](./integrazione-ouath2.md)
- Il sistema include un server **Keycloak** runnato in modalità PROD, nascosto dall'esterno della subnet ma esposto internamente alla porta `8081`
- Si appoggia su un database `postgress` per il salvataggio delle configurazioni
- Per interazioni dall'esterno della sottorete è stato posto un server Reverse-Proxy come layer intermediario tra l'esterno ed il server keycloak. Vai [qui](./keycloak-readme.md#8-configurazione-e-creazione-del-frontend-client-con-integrazione-per-reverse-proxy) per informazioni su come interrogarlo

### 7. Interfaccia Frontend spartana per le interazioni con l'architettura
- Scritto in HTML e Javascript puri per fornire una navigazione più scorrevole rispetto all'uso di un http-client, non il massimo della sicurezza ma trascurabile per i fini formativi del progetto
- Si interfaccia con il server di Authentication tramite server Reverse-Proxy esposto all'esterno della subnet su `localhost:8888`, che gestisce CORS e forwarding
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
- **Frontend**: http://localhost:4200

---

## 📜 Swagger dei Servizi Spring

- **API Gateway**: http://localhost:8080/
- **Demo Service**: http://localhost:8080/demo-service/

### Demo Service Swagger
```yaml
openapi: 3.0.1
info:
  title: Demo Service API
  description: API documentation for the Demo Service
  version: 1.0.0
paths:
  /api/public/demo:
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
  /api/private/user:
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
