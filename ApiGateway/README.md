Microservizio che smista il traffico ai servizi specifici, bilanciando il carico verso i server registrati in
EurekaServer e gestendo automaticamente il routing in maniera dinamica.  

- È basato su Spring Cloud Gateway.
- Fa routing dinamico verso i servizi registrati su Eureka.
- Fa bilanciamento del carico automatico tra le istanze dei servizi.
- Invia log verso Kafka.