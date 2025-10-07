package com.aremi.common.logging.extractor;

import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

public class WebFluxRequestExtractor implements RequestExtractor {

    @Override
    public String getMethod(Object[] args) {
        ServerWebExchange exchange = extract(args);
        return exchange != null ? exchange.getRequest().getMethod().name() : "unknown";
    }

    @Override
    public String getUri(Object[] args) {
        ServerWebExchange exchange = extract(args);
        return exchange != null ? exchange.getRequest().getURI().toString() : "unknown";
    }

    @Override
    public String getProtocol(Object[] args) {
        ServerWebExchange exchange = extract(args);
        return exchange != null ? exchange.getRequest().getURI().getScheme() : "unknown";
    }

    @Override
    public String getClientIp(Object[] args) {
        ServerWebExchange exchange = extract(args);
        if (exchange == null) return "unknown";
        return exchange.getRequest().getHeaders().getFirst("X-Real-IP");
    }

    @Override
    public String getStatus(Object result) {
        if (result instanceof ServerHttpResponse response && response.getStatusCode() != null) {
            return String.valueOf(response.getStatusCode().value());
        }
        return "unknown";
    }

    private ServerWebExchange extract(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ServerWebExchange exchange) {
                return exchange;
            }
        }
        return null;
    }
}
