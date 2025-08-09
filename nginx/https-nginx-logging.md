## 🎯 **Obiettivo**
Centralizzare l'invio dei log generati da NGINX verso Kafka, utilizzando un servizio Spring dedicato (`SpringKafkaProducer`) come unico punto di accesso al broker Kafka.

---

### 🧩 **Architettura scelta**

```plaintext
NGINX → Fluentd → KafkaProducer (Spring) → Kafka
```

#### ✅ **Motivazioni**
- **Separazione delle responsabilità**: NGINX si occupa solo di loggare, Fluentd di raccogliere, KafkaProducer di inviare.
- **Sicurezza**: Kafka non è esposto direttamente ai servizi esterni.
- **Scalabilità**: KafkaProducer può essere esteso per validazioni, arricchimenti, o routing su più topic.
- **Manutenibilità**: ogni componente può essere aggiornato o sostituito indipendentemente.

---

### 🛠️ **Configurazione NGINX**

#### ✅ Log in formato JSON
- Utilizzo di `log_format json_combined` per generare log strutturati.
- Scrittura su `/var/log/nginx/access.log` per compatibilità con Filebeat.

#### ✅ Campi inclusi nel log
| Campo JSON                | Descrizione                   |
|---------------------------|-------------------------------|
| `time`                    | Timestamp ISO della richiesta |
| `remote_addr`             | IP del client                 |
| `request`                 | Metodo + URI + protocollo     |
| `request_method`          | Metodo HTTP (GET, POST, ecc.) |
| `status`                  | Codice di risposta HTTP       |
| `body_bytes_sent`         | Byte inviati al client        |
| `http_referer`            | URL di provenienza            |
| `http_user_agent`         | User agent del client         |
| `request_time`            | Tempo totale di gestione      |
| `upstream_response_time`  | Tempo di risposta del backend |

#### ✅ Configurazione Docker
```yaml
read_only: true
tmpfs:
  - /tmp
volumes:
  - nginx-logs:/var/log/nginx
```

#### ✅ Motivazioni
- `read_only: true`: aumenta la sicurezza impedendo scritture non autorizzate.
- `tmpfs: /tmp`: garantisce compatibilità con processi che usano `/tmp`, senza persistenza (temporary filesystem => RAM).
- `nginx-logs`: volume dedicato per i log (in scrittura), accessibile da Filebeat.

---

### 🧠 **Ruolo di Fluentd**
Fluentd sarà configurato per:
- Leggere i log JSON da `/var/log/nginx/access.log`
- Inviarli via protocollo gRPC al servizio SpringKafkaProducer
- Non comunicare direttamente con Kafka

#### ✅ Motivazioni
- Evita coupling diretto tra Fluentd e Kafka.
- Permette di centralizzare la logica di invio Kafka in un microservizio.
- Facilita test, logging, e debugging lato Spring.

---

## 📜 1. Definizione `nginx_log.proto`

Abbiamo creato un file di definizione `.proto` per descrivere in modo strutturato il payload dei log in formato Protobuf.

### Struttura:

- `message NginxLog` rappresenta i dati principali del log: timestamp, IP client, richiesta HTTP, codice di stato, user-agent, e tempi di esecuzione.
- `service NginxLogReceiver` definisce il servizio gRPC che riceve i log in streaming.
- `rpc SendLog(NginxLog) returns (Ack)` è l’operazione esposta dal servizio.
- `message Ack` fornisce conferma della ricezione.

### 🎯 Motivazioni:
- **Protobuf + gRPC**: scelte per la loro efficienza, tipizzazione forte, e perfetta integrazione con ambienti Java/Spring.
- Separazione logica tra `NginxLog` e `Ack`: favorisce estensibilità futura.

---

## 🔧 2. Configurazione `fluent.conf`

La configurazione Fluentd ha due sezioni principali: `source` e `match`.

### `<source>`
- Tipo: `@type tail`
- Percorso: `/var/log/nginx/access.log`
- Formato: `json`
- `pos_file`: file di posizione per lettura incrementale
- `read_from_head`: garantisce lettura completa alla prima esecuzione

### `<match nginx.access>`
- Tipo: `@type grpc`
- Parametri gRPC:
    - `host`: nome del microservizio destinatario
    - `proto_path`: percorso del file `.proto`
    - `service_name` / `method_name`: corrispondenti alla definizione protobuf
- Buffering:
    - `flush_interval`: invii ogni 5 secondi per ottimizzare carico rete

### 🎯 Motivazioni:
- **Tail + JSON**: scelta naturale per file di log Nginx già in formato leggibile
- **gRPC come output**: consente una comunicazione binaria veloce e strutturata con il backend
- **Buffer configurato**: bilancia reattività e performance

---