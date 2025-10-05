# Multimedia Distributed Infrastructure with Spring, Eureka, API Gateway, Kafka, Docker and more

## 🧭 Overview

Questa infrastruttura software è progettata per supportare un ecosistema di microservizi distribuiti basati su **Spring Boot** e include:

- **Dominio** configurabile con singola modifica, leggi [qui](doc/domain-name-parametrico.md)
- **Service Discovery** tramite Eureka Server
- **SSL/HTTPS** tramite un Reverse-Proxy posto come unico punto di ingresso alla subnet, per una gestione centralizzata di HTTPS
- **API Gateway** per l'instradamento centralizzato delle richieste, posto dopo il Reverse-Proxy e non esposto verso l'esterno
- **Bilanciamento dinamico del carico** tramite Spring Cloud Gateway Webflux
- **Logging centralizzato** tramite Apache Kafka attraverso servizio SpringKafkaProducer
- **Containerizzazione con Docker e protezione container** per una gestione semplificata e [sicura](doc/domain-name-parametrico.md#-capitolo-3--sicurezza)
- **Estendibilità** per aggiungere nuovi servizi Spring in futuro
- **OAuth2** per la protezione delle risorse 
- **Frontend** spartano, integrato con il sistema di autenticazione e che in update futuri integrerà funzionalità di streaming MP3 e PDF
- **Protocollo gRPC** per le comunicazioni intranet, per la massima velocità e affidabilità

---

<big>**ATTENZIONE**</big>, per far funziona tutta l'infrastruttura <b><u>in localhost</u></b> bisogna modificare il file host della macchina inserendo un nuovo DNS, [guida](doc/ssl/https-readme.md#-guida-alla-modifica-del-file-hosts)

## 🧱 Architettura

### 1. Entrypoint con SSL/HTTPS
Gestione della sicurezza centralizzata tramite Reverse-Proxy HTTPS, che si occupa di cifrare tutte le comunicazioni in ingresso,
centralizzando la gestione dei certificati SSL.
Tutti i sistemi all'interno della sottorete usano lo stesso certificato SSL per cifrare le comunicazioni.  

Il Reverse-Proxy funge da unico punto ingresso per interagire con l'architettura, entrypoint su `https://multimedia-entrypoint/`
Più informazioni [qui](doc/ssl/reverse-proxy-doc.md) e più generali [qui](doc/ssl/reverse-proxy-general.md).  

E' disponibile un Frontend, dietro il Reverse-Proxy, che fornisce un'interfaccia utente per interagire con l'architettura.
accessibile su `https://multimedia-entrypoint/multimedia

### 2. Bilanciamento del carico
Il load balancing utilizza una combinazione delle seguenti tecnologie, ciascuna per risolvere uno specifico problema:
- **Eureka Server** per la gestione dei servizi e il routing delle richieste, più info [qui](./EurekaServer/README.md).
- **API Gateway** più info [qui](./ApiGateway/README.md), utilizza:
  - **Spring Cloud Gateway Webflux** per il routing delle richieste ai servizi downstream. Servizi che possono aggiungersi 
  o rimuoversi dinamicamente dal server Eureka, favorendo scaling orizzontale. Il routing avviene automaticamente tramite risoluzione DNS nell'URI.
  - **Spring Cloud LoadBalancer** che distribuisce le richieste tra le istanze disponibili e più libere.
  - Configurato come client Keycloak `confidential` per l'authentication.

#### Come funziona:
- Ogni servizio si registra su Eureka con un `serviceId`
- Il API-Gateway, con `discovery.locator.enabled=true`, rileva automaticamente i servizi
- Le richieste vengono indirizzate usando URI come: `lb://nome-servizio`
- Il LoadBalancer integrato distribuisce le richieste tra le istanze disponibili (round-robin di default)

### 3. Autenticazione & Autorizzazione - OAuth2
Integrato un sistema di autenticazione OAuth2 tramite server interno Keycloak, che si occupa di gestire l'autenticazione
e l'autorizzazione degli utenti.
- [Configurazione SEMI-AUTOMATICA server Keycloak](doc/auth/keycloak-import-guide.md)
- [Configurazione MANUALE server Keycloak](doc/auth/keycloak-readme.md)
- [Configurazione del Frontend-Client e del Reverse-Proxy](doc/auth/keycloak-readme.md#8-configurazione-e-creazione-del-frontend-client-con-integrazione-per-reverse-proxy)
- [Informazioni sul flusso](doc/auth/integrazione-ouath2.md)

### 4. Logging centralizzato con Kafka
- Il sistema include un **Kafka Producer Service** che centralizza le interazioni con `Kafka Broker`
- Ogni servizio/microservizio può pubblicare i propri messaggi su un topic dedicato (es. `log.request.service-name`) inviando via *gRPC* al **Kafka Producer Service**
- I log possono essere successivamente consumati da un'applicazione di monitoraggio o da un sistema di persistenza
- Le informazioni di Kafka sono visualizzabili tramite **Kafka UI** (browser)`.

### 5. Servizio Spring "demo-service"
Servizio Spring che fornisce un semplice endpoint per testare l'integrazione con Keycloak, più info [qui](./DemoService/README.md).
- Configurato come resource server `bearer-only` per l'authorization
- Presenta API pubbliche e private, anche role-based,

### 6. Servizio Spring "music-streaming-service"


---

## 🔍 Monitoraggio

- **EurekaServer Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:8085
- **Keycloak Dashboard**: tramite *NGINX Reverse-Proxy* su https://multimedia-entrypoint
- **Frontend**: tramite *NGINX Reverse-Proxy* su https://multimedia-entrypoint/multimedia

---

## 📜 Swagger dei Servizi Spring

- **API Gateway**: **/api
- **Demo Service**: **/api/demo-service/

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
