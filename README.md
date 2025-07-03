🧭 Prossimi step possibili
Ecco cosa potresti aggiungere da qui:

Fase	Obiettivo	Tecnologie
✅	Routing dinamico + bilanciamento	Spring Cloud Gateway
✅	Aggiunta di un servizio demo dietro il gateway	Spring Boot REST service
✅	Integrazione docker-compose completo	Docker
✅	Logging centralizzato (ELK o Grafana Loki)	Fluent Bit, ELK
🔜	Sicurezza (JWT, Auth server)	Spring Security, OAuth2
🔜	API Rate Limiting / Caching	Resilience4j, Redis, Spring Cloud Gateway filters


- localhost:8080 per interagire con il ApiGateway
- locahost:8761 per vedere i servizi registrati su Eureka
- localhost:8085 per vedere le informazioni topic/message di Kafka