package com.aremi.common.logging.aspect;

import com.aremi.common.logging.conf.RequestExtractorAutoConfiguration;
import com.aremi.common.logging.extractor.RequestExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import io.grpc.StatusRuntimeException;
import requestlog.RequestLogOuterClass;
import requestlog.RequestLogReceiverGrpc;

import java.time.Instant;

@AutoConfiguration(after = RequestExtractorAutoConfiguration.class)
@ConditionalOnBean(RequestExtractor.class)
@Slf4j
@Aspect
@Component
//@RequiredArgsConstructor
public class RequestLoggingAspect {
    public RequestLoggingAspect(RequestExtractor extractor, RequestLogReceiverGrpc.RequestLogReceiverBlockingStub stub) {
        log.info("debug______ aspect inizializzato");
        this.extractor = extractor;
        this.stub = stub;
    }

    private final RequestExtractor extractor;
    private final RequestLogReceiverGrpc.RequestLogReceiverBlockingStub stub;

    @org.springframework.beans.factory.annotation.Value("${app.kafka-topic.request:log.request.default}")
    private String topic;

    @org.springframework.beans.factory.annotation.Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Around("execution(* com..controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("debug______ aspect partito");
        var start = Instant.now();

        Object result = joinPoint.proceed();

        var end = Instant.now();
        var durationMs = end.toEpochMilli() - start.toEpochMilli();

        RequestLogOuterClass.RequestLog logRequest = RequestLogOuterClass.RequestLog.newBuilder()
                .setTime(start.toString())
                .setRemoteAddr(extractor.getClientIp(joinPoint.getArgs()))
                .setRequest(extractor.getMethod(joinPoint.getArgs()) + " " +
                        extractor.getUri(joinPoint.getArgs()) + " " +
                        extractor.getProtocol(joinPoint.getArgs()))
                .setStatus(extractor.getStatus(result))
                .setRequestTime(durationMs + "ms")
                .setService(applicationName)
                .setKafkaTopic(topic)
                .build();

        try {
            RequestLogOuterClass.Ack ack = stub.sendLog(logRequest);
            if (ack.getStatus() > 0) {
                log.info("Log inviato per {}", extractor.getUri(joinPoint.getArgs()));
            } else {
                log.error("Errore nell'invio del log: {}", ack.getMessage());
            }
        } catch (StatusRuntimeException e) {
            log.error("Errore gRPC durante l'invio del log", e);
        }

        return result;
    }
}
