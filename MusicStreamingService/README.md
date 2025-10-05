# Configurazioni di redis.conf

```conf
maxmemory 512mb                     # limite massimo di RAM che Redis può usare
maxmemory-policy allkeys-lfu       # strategia di eviction: Least Frequently Used
save ""                             # disabilita snapshot RDB
appendonly no                       # disabilita AOF
requirepass supersecret             # password per autenticazione
```