# Documentazione Reverse-Proxy

- Unico punto di ingresso alla subnet e all'infrastruttura, espone su `https://multimedia-entrypoint/`
- Permette di applicare agevolmente filtri, controlli di sicurezza e limitazioni del traffico in entrata (DA INTEGRARE)
- Log-Forwarder in Python e verso SpringKafkaProducer per monitorare tutte le richieste in entrata, più [info](forwarder.md)
- Informazioni sulla creazione del certificato SSL [qui](https-readme.md)