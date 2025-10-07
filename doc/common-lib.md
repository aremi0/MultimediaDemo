# 📘 Documentazione: Integrazione libreria `common-logging`

## 1. Problema iniziale
- Ogni microservizio aveva bisogno di **logging centralizzato** delle richieste.
- L’obiettivo era inviare i log a un servizio gRPC (KafkaProducerService) che li inoltra a Kafka.
- Senza una libreria comune, ogni servizio avrebbe dovuto duplicare configurazioni, aspect e stub gRPC.

---

## 2. Obiettivo
- Creare una libreria **riutilizzabile e plug‑and‑play** (`common-logging`) che:
    - intercetti automaticamente le richieste (MVC o WebFlux);
    - estragga le informazioni principali (metodo, URI, IP, status, tempo di risposta);
    - invii i log via gRPC al servizio centralizzato;
    - sia configurabile tramite `application.yml` del microservizio, senza codice aggiuntivo.

---

## 3. Micro‑problemi incontrati e soluzioni

### 3.1 Conflitto tra `MvcRequestExtractor` e `WebFluxRequestExtractor`
- **Problema**: Spring trovava due bean `RequestExtractor` (uno per MVC e uno per WebFlux).
- **Soluzione**: sostituite le condizioni con `@ConditionalOnWebApplication`:
    - `@ConditionalOnWebApplication(type = SERVLET)` per MVC.
    - `@ConditionalOnWebApplication(type = REACTIVE)` per WebFlux.  
      → Così viene registrato **solo l’estrattore corretto** in base al tipo di app.

### 3.2 Auto‑configurazione della libreria
- **Problema**: volevamo che la libreria fosse plug‑and‑play, senza `@Import` manuali.
- **Soluzione**: aggiunto file `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` con le classi da registrare (`RequestLogConfigurer`, `RequestLoggingAspect`, gli extractor).

### 3.3 Avvio del API-Gateway
- **Problema**: lo starter `spring-cloud-starter-gateway-server-webflux` include anche l’autoconfigurazione gRPC, necessaria al filtro custom `GrpcLoggingFilter`.
- **Soluzione**: aggiunto la dipendenza `grpc-netty`
- **Osservazione**:  È basato su Spring Cloud Gateway, che a sua volta è costruito sopra Spring WebFlux.
Quando parte, il contesto Spring viene riconosciuto come applicazione Web reattiva. Questo fa sì che la condizione @ConditionalOnWebApplication(type = REACTIVE) sia vera → quindi viene creato un bean WebFluxRequestExtractor.
Risultato: l’Aspect trova un RequestExtractor da iniettare e non esplode, anche se non hai controller espliciti.

### 3.4 Avvio del Kafka-Producer-Service
- **Problema**: la libreria `common-logging` registra sempre l’*Aspect* (`RequestLoggingAspect`), che richiede un bean `RequestExtractor` (*MVC* o *WebFlux*).
Nel KafkaProducerService non ci sono controller HTTP → quindi nessun RequestExtractor viene creato, quindi non vi è nessun candidato per l’iniezione.
- **Soluzione**: reso condizionale l'*Aspect* nella libreria `common-logging` (`@ConditionalOnBean(RequestExtractor.class)`).
- **Osservazione**: È un servizio gRPC puro, non ha né MVC né WebFlux. Il contesto Spring non viene visto come “web application” (né SERVLET né REACTIVE).
Quindi nessun RequestExtractor viene registrato. Risultato: l’Aspect non trova nulla da iniettare → UnsatisfiedDependencyException.

### 3.5 Avvio del generico Servizio (Demo-Service o Music-Streaming-Service)
- **Problema**: non viene inizializzato il Bean `RequestLoggingAspect` della libreria `common-logging`. Questo era annotato
con `@ConditionalOnBean(RequestExtractor.class)`, ma al momento della sua valutazione Spring Boot non aveva ancora registrato alcun bean `RequestExtractor`.
Questo accadeva perché gli `Extractor` venivano dichiarati in una configurazione separata, ma l’ordine di caricamento delle auto‑configuration faceva sì che
l’*Aspect* venisse processato troppo presto, fallendo la condizione.
- **Soluzione**: Per risolvere il problema è stato necessario definire i Bean tramite classe @Configuration anzichè direttamente come @Component,
forzando poi l’ordine di inizializzazione con l’annotazione `@AutoConfiguration(after = RequestExtractorAutoConfiguration.class)` sulla medesima classe @Configuration,
in modo che l’aspect venga valutato solo dopo la creazione dei bean *RequestExtractor*. E infine impostando l'ordine corretto dei Bean 
nel file `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` della libreria.

---

## 4. Soluzione finale

### 4.1 Libreria `common-logging`
- Contiene:
    - Aspect `RequestLoggingAspect` che intercetta i controller.
    - Configurazione gRPC `RequestLogConfigurer`.
    - Estrattori MVC/WebFlux condizionati.
    - File `AutoConfiguration.imports` per auto‑registrazione.
- Espone solo le properties necessarie:
  ```yaml
  app:
    kafka-producer:
      host: spring-kafka-producer
      port: 6565
    kafka-topic:
      request: log.request.default
  spring:
    application:
      name: my-service
  ```

### 4.2 Microservizi client (es. DemoService, ApiGateway, MusicStreamingService)
- POM semplificato: aggiungono solo la dipendenza a `common-logging`.
- Non hanno più plugin Protobuf o dipendenze gRPC di basso livello.
- Usano solo gli starter Spring necessari (Web, WebFlux, Security, ecc.).
- Logging attivo automaticamente.

### 4.3 Servizio ricevente (KafkaProducerService)
- Dipende da `common-logging` per riutilizzare le classi generate dal `.proto`.
- Implementa il servizio gRPC `RequestLogReceiverImplBase`.
- Invia i log ricevuti a Kafka.

---

## 5. Risultato
- **Architettura pulita**: i `.proto` e la logica di logging sono centralizzati.
- **Microservizi leggeri**: ognuno importa solo `common-logging` e configura le properties.
- **Plug‑and‑play**: nessuna configurazione manuale, nessun `@Import`.
- **Compatibilità**: funziona sia con MVC che con WebFlux, senza conflitti.
- **Scalabilità**: facile aggiungere nuovi microservizi, basta la dipendenza.

---
