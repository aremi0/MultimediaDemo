import grpc
import json
import os
import time
import logging
import re
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from request_log_pb2 import RequestLog
from request_log_pb2_grpc import RequestLogReceiverStub

REQUEST_KAFKA_TOPIC = os.getenv("REQUEST_KAFKA_TOPIC", "log.request.nginx-entrypoint")
LOG_PATH = os.getenv("LOG_PATH", "/var/log/nginx/access_real.log")
GRPC_TARGET = os.getenv("GRPC_TARGET", "spring-kafka-producer:6565")
HEARTBEAT_INTERVAL = int(os.getenv("HEARTBEAT_INTERVAL", "60"))  # default: 60 sec

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)

def is_relevant(log_json):
    request = log_json.get("request", "")

    patterns = [
        r"GET /realms/multimedia-realm/protocol/openid-connect/auth\?",
        r"POST /realms/multimedia-realm/login-actions/authenticate\?",
        r"POST /realms/multimedia-realm/protocol/openid-connect/token",
        r"GET /realms/master/protocol/openid-connect/auth\?",
    ]

    for pattern in patterns:
        if re.match(pattern, request):
            return True

    return False

class LogHandler(FileSystemEventHandler):
    def __init__(self, stub):
        self.stub = stub
        self.last_size = 0
        self.logs_sent = 0

    def on_modified(self, event):
        if event.src_path != LOG_PATH:
            return

        try:
            current_size = os.path.getsize(LOG_PATH)
            if current_size < self.last_size:
                # Log rotated
                self.last_size = 0

            with open(LOG_PATH, "r") as f:
                f.seek(self.last_size)
                new_data = f.read()
                self.last_size = current_size

            for line in new_data.strip().split("\n"):
                if not line.strip():
                    continue
                try:
                    log_json = json.loads(line)
                    if not is_relevant(log_json):
                        logging.debug(f"⏭️ Log ignorato: {log_json.get('request', '')}")
                        continue

                    log_msg = RequestLog(
                        time=log_json.get("time", ""),
                        remote_addr=log_json.get("remote_addr", ""),
                        request=log_json.get("request", ""),
                        status=log_json.get("status", ""),
                        request_time=log_json.get("request_time", ""),
                        service="nginx-entrypoint",
                        kafka_topic=REQUEST_KAFKA_TOPIC
                    )
                    ack = self.stub.SendLog(log_msg)
                    self.logs_sent += 1
                    logging.debug(f"✅ Sent log: {log_msg.time} → Ack: {ack.message}")
                except Exception as e:
                    logging.warning(f"⚠️ Error parsing/sending log: {e}")

        except Exception as e:
            logging.error(f"⚠️ Error reading log file: {e}")

def main():
    channel = grpc.insecure_channel(GRPC_TARGET)
    stub = RequestLogReceiverStub(channel)

    event_handler = LogHandler(stub)
    observer = Observer()
    observer.schedule(event_handler, path=os.path.dirname(LOG_PATH), recursive=False)
    observer.start()

    logging.info("🚀 Log forwarder started.")
    try:
        last_heartbeat = time.time()
        while True:
            time.sleep(1)
            now = time.time()
            if now - last_heartbeat >= HEARTBEAT_INTERVAL:
                logging.info(f"💡 Log forwarder attivo — log inviati finora: {event_handler.logs_sent}")
                last_heartbeat = now
    except KeyboardInterrupt:
        logging.info("🛑 Interruzione manuale ricevuta. Arresto in corso...")
        observer.stop()
    observer.join()

if __name__ == "__main__":
    main()
