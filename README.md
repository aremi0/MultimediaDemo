# Spring Distributed Infrastructure with Eureka, API Gateway, Kafka Logging and Docker

## 🧭 Overview

Questa infrastruttura software è progettata per supportare un ecosistema di microservizi distribuiti basati su **Spring Boot**. Include:

- **Service Discovery** tramite Eureka Server
- **API Gateway** per l'instradamento centralizzato delle richieste
- **Bilanciamento dinamico del carico** tramite Spring Cloud Gateway
- **Logging centralizzato** tramite Apache Kafka
- **Containerizzazione con Docker** per una gestione semplificata
- **Estendibilità** per aggiungere nuovi servizi Spring in futuro

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

---

## ⚖️ Bilanciamento dinamico con Spring Cloud Gateway

Spring Cloud Gateway utilizza **Spring Cloud LoadBalancer** per distribuire dinamicamente le richieste tra le istanze dei servizi registrati su Eureka.

### Come funziona:
- Ogni servizio si registra su Eureka con un `serviceId`
- Il Gateway, con `discovery.locator.enabled=true`, rileva automaticamente i servizi
- Le richieste vengono indirizzate usando URI come: `lb://nome-servizio`
- Il LoadBalancer integrato distribuisce le richieste tra le istanze disponibili (round-robin di default)

---

## 🔍 Servizi di Monitoraggio

- **EurekaServer Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:8085

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
  /api/demo:
    get:
      summary: Get Demo
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
