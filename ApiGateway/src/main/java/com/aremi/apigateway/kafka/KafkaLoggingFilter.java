package com.aremi.apigateway.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaLoggingFilter implements WebFilter {
    private final String TOPIC = "gateway-log";
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Prima di passare la richiesta avanti, registra info
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        Instant timestamp = Instant.now();

        // Estrai gli header come mappa semplice (da MultiValueMap a Map<String, String>)
        Map<String, String> headersMap = new HashMap<>();
        exchange.getRequest().getHeaders().forEach((key, values) -> {
            // Unisci i valori multipli in una singola stringa separata da virgole
            headersMap.put(key, String.join(",", values));
        });

        Map<String, Object> logMap = new HashMap<>();
        logMap.put("method", method);
        logMap.put("path", path);
        logMap.put("headers", headersMap);
        logMap.put("timestamp", timestamp.toString());

        // Trasforma la mappa in JSON (puoi usare la tua libreria preferita)
        String logJson = toJson(logMap);

        // Invia a Kafka (la chiave può essere nulla o qualcosa come method)
        kafkaTemplate.send(TOPIC, method, logJson);

        // Continua il filtro
        return chain.filter(exchange);
    }

    private String toJson(Map<String, Object> map) {
        // Per semplicità uso manuale (ma puoi usare Jackson o Gson)
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k,v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
