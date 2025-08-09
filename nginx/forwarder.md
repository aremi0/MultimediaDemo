## 🎯 **Obiettivo**
Centralizzare l'invio dei log generati da NGINX verso Kafka, utilizzando un servizio Spring dedicato (`SpringKafkaProducer`) come unico punto di accesso al broker Kafka.

---

### 🧩 **Architettura scelta**

```plaintext
NGINX → Script Python per Log-Forwarding → KafkaProducer (Spring) → Kafka
```

#### ✅ **Motivazioni**
- **Separazione delle responsabilità**: NGINX si occupa di loggare, Python di leggere e mandare a KafkaProducer che invia a Kafka.
- **Sicurezza**: Kafka non è esposto direttamente ai servizi esterni.
- **Scalabilità**: KafkaProducer può essere esteso per validazioni, arricchimenti, o routing su più topic.
- **Manutenibilità**: ogni componente può essere aggiornato o sostituito indipendentemente.

---

### 🛠️ **Configurazione NGINX**

#### ✅ Log in formato JSON
- Utilizzo di `log_format json_combined buffer=32k flush=5s` per generare log strutturati con buffer di 32kb e flush ogni 5s anche se il buffer non è pieno.
- Scrittura su file fisico `/var/log/nginx/access_real.log` in quanto l'originale *access.log* è un symlink verso *std_out*.

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
- `nginx-logs`: volume dedicato per i log (in scrittura), accessibile da Python.

---

## 🐍 Ruolo dello script Python: `log-forwarder`

### 🎯 Obiettivo

> Garantire che ogni richiesta HTTP gestita da Nginx venga tracciata, convertita in un messaggio strutturato, e trasmessa in tempo reale al backend Kafka tramite gRPC.

---

## 🔄 Funzionamento

1. **Monitoraggio del file di log**: tramite `watchdog` reagisce ogni volta che Nginx scrive una nuova riga al file `/var/log/nginx/access_real.log`
2. **Parsing del log**: converte in un oggetto `NginxLog` definito nel `.proto`
3. **Invio via gRPC**: invia il log tramite il metodo `SendLog` del servizio Spring (`spring-kafka-producer:6565`)
4. **Logging e metriche**: stampa log periodici per confermare che è attivo a intervalli regolabili via variabile d’ambiente (`HEARTBEAT_INTERVAL`)

---

## 🛡️ Considerazioni

- Il file di log deve essere scritto fisicamente da Nginx (non solo su stdout)
- Il formato JSON deve essere coerente con il `.proto`
- Il container Python deve avere accesso al file tramite volume condiviso

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

Perfetto Gabriele! Ecco un **README breve e chiaro** che riassume gli aggiornamenti da fare al tuo sistema, suddiviso per componente: `nginx_log.proto`, script Python, producer Kafka e configurazione Nginx.

---

# 📘 Aggiornamenti per log forwarding via gRPC + Kafka

## 🧬 1. Aggiornamenti al file `nginx_log.proto`

### ✅ Aggiunta campo `kafka_topic`
Modifica il tuo `.proto` per includere un nuovo campo stringa:

```proto
message NginxLog {
  string time = 1;
  string remote_addr = 2;
  string request = 3;
  string status = 4;
  string http_user_agent = 5;
  string request_time = 6;
  string upstream_response_time = 7;
  string kafka_topic = 8; // 🆕 nuovo campo
}
```

> Questo campo sarà **popolato dal servizio Python** che invia il messaggio gRPC, in base al path della richiesta.

---

## 🐍 2. Aggiornamenti allo script Python

### ✅ Determinazione del `kafka_topic` in base al path

Aggiungi una funzione per estrarre il topic dalla richiesta:

```python
def extract_kafka_topic(request):
    if "/realms/master/" in request:
        return "realm-master"
    elif "/realms/multimedia-realm/" in request:
        return "realm-multimedia"
    elif "/admin/" in request:
        return "admin-console"
    else:
        return "generic-log"
```

Poi nel blocco di invio:

```python
log_msg = NginxLog(
    time=log_json.get("time", ""),
    remote_addr=log_json.get("remote_addr", ""),
    request=log_json.get("request", ""),
    status=log_json.get("status", ""),
    http_user_agent=log_json.get("http_user_agent", ""),
    request_time=log_json.get("request_time", ""),
    upstream_response_time=log_json.get("upstream_response_time", ""),
    kafka_topic=extract_kafka_topic(log_json.get("request", ""))
)
```

---

## 🚀 3. Aggiornamenti al producer Kafka

### ✅ Uso del campo `kafka_topic` per il routing

Il producer Kafka deve:
- Leggere il campo `kafka_topic` dal messaggio gRPC.
- Inviare il messaggio al topic corrispondente.
- Gestire topic dinamici o predefiniti (fallback su `generic-log` se vuoto).

---

## ⚙️ 4. Revisione dei campi nel log Nginx

### 🔍 Analisi consigliata

Valuta se i seguenti campi sono utili o ridondanti:

| Campo Nginx             | Utile per Kafka? | Note |
|-------------------------|------------------|------|
| `time`                  | ✅               | Timestamp utile |
| `remote_addr`           | ✅               | IP client |
| `request`               | ✅               | Serve per filtrare e determinare il topic |
| `status`                | ✅               | Per analisi di successo/errore |
| `http_user_agent`       | ❓               | Utile solo se fai analisi su device/browser |
| `request_time`          | ✅               | Performance |
| `upstream_response_time`| ✅               | Performance backend |

### 🧪 Campi aggiuntivi utili

- `http_referer`: per tracciare origine della richiesta
- `host`: utile se Nginx gestisce più virtual host
- `method`: separato da `request` per analisi più semplice

> Puoi aggiungerli nel formato JSON del log Nginx e nel `.proto` se necessario.

---

## ✅ Conclusione

| Componente      | Azione da fare                         |
|-----------------|----------------------------------------|
| `nginx_log.proto` | Aggiungere campo `kafka_topic`        |
| Python script     | Determinare `kafka_topic` dal path    |
| Kafka producer    | Usare `kafka_topic` per routing       |
| Nginx log config  | Rivedere campi log e aggiungere utili |

---
