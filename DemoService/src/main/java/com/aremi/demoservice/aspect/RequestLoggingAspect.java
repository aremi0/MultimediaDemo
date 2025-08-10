package com.aremi.demoservice.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import requestlog.RequestLogOuterClass;
import requestlog.RequestLogReceiverGrpc;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestLoggingAspect {
    @Value("${app.kafka-topic.request:log.request.demo-service}")
    private String topic;

    private final RequestLogReceiverGrpc.RequestLogReceiverBlockingStub stub;

    @Around("execution(* com.aremi.demoservice.controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        var start = Instant.now();

        var request = extractHttpServletRequest(joinPoint.getArgs());
        if (Objects.isNull(request)) {
            return joinPoint.proceed(); // Skip if no HttpServletRequest is injected
        }

        var method = request.getMethod();
        var uri = request.getRequestURI();
        var protocol = request.getProtocol();
        var clientIp = getClientIp(request);

        var result = joinPoint.proceed();

        var end = Instant.now();
        var duration = Duration.between(start, end);

        var statusCode = extractStatusCode(result);

        RequestLogOuterClass.RequestLog logRequest = RequestLogOuterClass.RequestLog.newBuilder()
            .setTime(start.toString())
            .setRemoteAddr(!Objects.isNull(clientIp) ? clientIp : "unknown")
            .setRequest(method + " " + uri + " " + protocol)
            .setStatus(statusCode)
            .setRequestTime(duration + "ms")
            .setService("demo-gateway")
            .setKafkaTopic(topic)
            .build();

        RequestLogOuterClass.Ack ack = stub.sendLog(logRequest);
        if (ack.getStatus() > 0) {
            log.info("Ricevuta request per '{}' e log inviato", uri);
        } else {
            log.error("Errore nell'invio del log: {}", ack.getMessage());
        }

        return result;
    }

    private HttpServletRequest extractHttpServletRequest(Object[] args) {
        for (var arg : args) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }
        return null;
    }

    private String extractStatusCode(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            return String.valueOf(response.getStatusCode().value());
        }
        return "unknown"; // default fallback
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Real-IP");
        if (!Objects.isNull(xfHeader)) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
