Certamente Gabriele! 📘  
Ecco la **documentazione tecnica** completa del modulo `spring-kafka-producer`, organizzata per sezioni, con tutte le caratteristiche principali, inclusa la gestione delle variabili d’ambiente con fallback.

---

## 🧩 Panoramica del modulo

| Caratteristica                  | Descrizione                                                                              |
|---------------------------------|------------------------------------------------------------------------------------------|
| **Nome progetto**               | `spring-kafka-producer`                                                                  |
| **Ruolo**                       | Riceve log via gRPC da Fluentd **e da altri client compatibili**, e li pubblica su Kafka |
| **Protocollo di comunicazione** | gRPC (porta 6565)                                                                        |
| **Tecnologie principali**       | Spring Boot, gRPC, Kafka, Docker                                                         |
| **Schema dati**                 | `.proto` ➜ classi Java ➜ JSON serializzato su Kafka                                      |
| **Deployment**                  | Container Docker avviato tramite `docker-compose`                                        |
| **Configurazione runtime**      | Completamente via `application.properties` o variabili d’ambiente                        |

---

## ⚙️ Proprietà applicative (`application.properties`)

### 🔹 gRPC

```properties
server.port=0                        # Disattiva HTTP classico
grpc.server.port=6565                # Porta gRPC
grpc.server.security.enabled=false   # Nessuna sicurezza TLS lato server
```

### 🔹 Kafka

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.properties.acks=all
spring.kafka.producer.properties.retries=3
spring.kafka.producer.properties.linger.ms=5
```

> ⚠️ Le proprietà usano la sintassi `${VARIABILE:default}` che indica:  
> “leggi la variabile d’ambiente, altrimenti usa il valore di default”.

### 🔹 Proprietà custom

```properties
app.kafka.topic=${KAFKA_TOPIC:nginx.logs}
```

> ✳️ `app.kafka.topic` può essere iniettata via `@Value` oppure tramite `@ConfigurationProperties`.

---

## 📦 Componente gRPC: `NginxLogReceiverService`

| Responsabilità                 | Descrizione                                                         |
|--------------------------------|---------------------------------------------------------------------|
| Annotation principale          | `@GrpcService`                                                      |
| Integrazione Kafka             | Usa `KafkaTemplate` auto-configurato per inviare messaggi           |
| Serializzazione log            | `ObjectMapper` converte il log da gRPC in JSON                      |
| Chiave Kafka                   | Usa l’indirizzo IP remoto come chiave (partitioning logico)         |
| Ack response                   | Restituisce `Ack` con messaggio "OK" o "ERROR: ..."                 |
| Gestione POJO interno          | Usa un `record` Java per rappresentare il log                       |

Esempio di invio su Kafka:

```java
var key = request.getRemoteAddr();
var json = objectMapper.writeValueAsString(toPojo(request));
kafkaTemplate.send(topic, key, json);
```

---

## 🐳 Docker & Runtime

| Componente Docker             | Configurazione                           |
|-------------------------------|------------------------------------------|
| `spring-kafka-producer`       | Avviato via Docker Compose               |
| Variabili passate via Compose | `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_TOPIC` |
| Porta esposta nel network     | gRPC: `6565`                             |
| JAR spring montato            | `/app.jar`                               |

---

## 🧠 Comportamenti notevoli

- ✅ Non usa HTTP Spring standard → `server.port=0`
- ✅ Supporta variabili d’ambiente con fallback → `${...:default}`
- ✅ Non richiede una `KafkaConfig` custom grazie all’autoconfigurazione Spring
- ✅ Facile da estendere per supportare altri formati log o headers gRPC
- ✅ Chiave Kafka personalizzabile (es. per analytics o stream partitioning)

---
