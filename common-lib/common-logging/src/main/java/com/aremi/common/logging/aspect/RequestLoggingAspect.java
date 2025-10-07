package com.aremi.common.logging.aspect;

import com.aremi.common.logging.conf.RequestExtractorAutoConfiguration;
import com.aremi.common.logging.extractor.RequestExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import io.grpc.StatusRuntimeException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import requestlog.RequestLogOuterClass;
import requestlog.RequestLogReceiverGrpc;

import java.time.Instant;

/**
 * Aspect che intercetta i metodi dei controller per registrare i log delle richieste.
 *
 * <p>Funziona sia in contesti MVC (Servlet) che in contesti WebFlux (reattivi).
 * In MVC gli argomenti del metodo contengono l'oggetto {@code HttpServletRequest},
 * mentre in WebFlux il {@link ServerWebExchange} viene recuperato dal Reactor Context
 * grazie al filtro {@link com.aremi.common.logging.conf.ExchangeContextConfiguration}.</p>
 *
 * <p>Il log viene inviato tramite gRPC a un servizio esterno, includendo informazioni
 * come metodo HTTP, URI, protocollo, indirizzo IP client, status e durata della richiesta.</p>
 */

@AutoConfiguration(after = RequestExtractorAutoConfiguration.class)
@ConditionalOnBean(RequestExtractor.class)
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestLoggingAspect {

    private final RequestExtractor extractor;
    private final RequestLogReceiverGrpc.RequestLogReceiverBlockingStub stub;

    @Value("${app.kafka-topic.request:log.request.default}")
    private String topic;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    /**
     * Intercetta i metodi dei controller e applica la logica di logging.
     *
     * @param joinPoint il punto di esecuzione del metodo
     * @return il risultato originale del metodo (Mono, Flux o oggetto MVC)
     * @throws Throwable se il metodo target lancia eccezioni
     */
    @Around("execution(* com..controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("debug______ aspect partito");
        var start = Instant.now();

        Object result = joinPoint.proceed();

        /**
         * In contesti WebFlux il controller restituisce Mono/Flux, quindi il risultato non è immediatamente disponibile
         * (chiamata asincrona).
         * Ogni volta che il Mono emette un segnale (onNext, onError, onComplete, onSubscribe),
         * guarda nel Reactor Context: se c’è un ServerWebExchange, usalo per chiamare sendLog e costruire il log della richiesta.
         */
        if (result instanceof Mono<?> mono) {
            return mono.doOnEach(signal -> {
                if (signal.isOnComplete() || signal.isOnError()) {
                    signal.getContextView()
                            .<ServerWebExchange>getOrEmpty(ServerWebExchange.class)
                            .ifPresent(exchange -> sendLog(joinPoint, start, exchange));
                }
            });
        } else if (result instanceof Flux<?> flux) {
            return flux.doOnEach(signal -> {
                if (signal.isOnComplete() || signal.isOnError()) {
                    signal.getContextView()
                            .<ServerWebExchange>getOrEmpty(ServerWebExchange.class)
                            .ifPresent(exchange -> sendLog(joinPoint, start, exchange));
                }
            });
        } else {
            // caso MVC
            sendLog(joinPoint, start, result);
            return result;
        }
    }

    /**
     * Costruisce e invia il log della richiesta.
     *
     * <p>In WebFlux il {@code result} è un {@link ServerWebExchange}, mentre in MVC
     * contiene il risultato del metodo (es. {@code ResponseEntity}).</p>
     *
     * @param joinPoint il punto di esecuzione del metodo
     * @param start     l'istante di inizio della richiesta
     * @param result    il risultato del metodo o l'exchange in WebFlux
     */
    private void sendLog(ProceedingJoinPoint joinPoint, Instant start, Object result) {
        var end = Instant.now();
        var durationMs = end.toEpochMilli() - start.toEpochMilli();

        Object[] argsForExtractor;
        Object statusSource;
        if (result instanceof ServerWebExchange exchange) {
            argsForExtractor = new Object[]{exchange};
            statusSource = exchange;
        } else {
            argsForExtractor = joinPoint.getArgs();
            statusSource = result;
        }

        RequestLogOuterClass.RequestLog logRequest = RequestLogOuterClass.RequestLog.newBuilder()
                .setTime(start.toString())
                .setRemoteAddr(extractor.getClientIp(argsForExtractor))
                .setRequest(extractor.getMethod(argsForExtractor) + " " +
                        extractor.getUri(argsForExtractor) + " " +
                        extractor.getProtocol(argsForExtractor))
                .setStatus(extractor.getStatus(statusSource))
                .setRequestTime(durationMs + "ms")
                .setService(applicationName)
                .setKafkaTopic(topic)
                .build();

        try {
            RequestLogOuterClass.Ack ack = stub.sendLog(logRequest);
            if (ack.getStatus() > 0) {
                log.info("Log inviato per {}", extractor.getUri(argsForExtractor));
            } else {
                log.error("Errore nell'invio del log: {}", ack.getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("Errore gRPC durante l'invio del log", e);
        }
    }
}
