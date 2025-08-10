package com.aremi.apigateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import requestlog.RequestLogOuterClass;
import requestlog.RequestLogReceiverGrpc;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcLoggingFilter implements WebFilter {

    private final RequestLogReceiverGrpc.RequestLogReceiverBlockingStub stub;

    @Value("${app.kafka-topic.request:log.request.api-gateway}")
    private String topic;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant start = Instant.now();

        return chain.filter(exchange)
            .doFinally((signalType) -> {
                var end = Instant.now();
                var durationMs = end.toEpochMilli() - start.toEpochMilli();
                var path = exchange.getRequest().getURI().getPath();

                RequestLogOuterClass.RequestLog logRequest = RequestLogOuterClass.RequestLog.newBuilder()
                    .setTime(start.toString())
                    .setRemoteAddr(!Objects.isNull(exchange.getRequest().getRemoteAddress()) ?
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown")
                    .setRequest(exchange.getRequest().getMethod().name() + " " +
                            path + " HTTP/1.1")
                    .setStatus(!Objects.isNull(exchange.getResponse().getStatusCode()) ?
                        String.valueOf(exchange.getResponse().getStatusCode().value()) : "unknown")
                    .setRequestTime(durationMs + "ms")
                    .setService("api-gateway")
                    .setKafkaTopic(topic)
                    .build();

                RequestLogOuterClass.Ack ack = stub.sendLog(logRequest);
                if (ack.getStatus() > 0) {
                    log.info("Ricevuta request per '{}' e log inviato", path);
                } else {
                    log.error("Errore nell'invio del log: {}", ack.getMessage());
                }
            });
    }
}
