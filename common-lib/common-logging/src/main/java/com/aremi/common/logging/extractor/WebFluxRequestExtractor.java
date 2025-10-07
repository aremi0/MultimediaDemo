package com.aremi.common.logging.extractor;

import org.springframework.web.server.ServerWebExchange;

/**
 * Implementazione di {@link RequestExtractor} per applicazioni WebFlux.
 *
 * <p>Estrae le informazioni della richiesta a partire da un
 * {@link ServerWebExchange}, come metodo HTTP, URI, protocollo,
 * indirizzo IP del client e status della risposta.</p>
 *
 * <p>In WebFlux gli argomenti del metodo del controller non contengono
 * l'exchange, quindi l'aspect passa direttamente l'oggetto
 * {@link ServerWebExchange} come parametro.</p>
 */
public class WebFluxRequestExtractor implements RequestExtractor {

    /**
     * Restituisce il metodo HTTP della richiesta.
     *
     * @param args array che contiene il {@link ServerWebExchange}
     * @return il metodo HTTP oppure "unknown" se non disponibile
     */
    @Override
    public String getMethod(Object[] args) {
        if (args.length > 0 && args[0] instanceof ServerWebExchange exchange) {
            return exchange.getRequest().getMethod().name();
        }
        return "unknown";
    }

    /**
     * Restituisce il path della richiesta (con eventuale query string).
     *
     * @param args array che contiene il {@link ServerWebExchange}
     * @return il path della richiesta (es. "/api/demo" o "/api/demo?foo=bar"),
     *         oppure "unknown" se non disponibile
     */
    @Override
    public String getUri(Object[] args) {
        if (args.length > 0 && args[0] instanceof ServerWebExchange exchange) {
            String path = exchange.getRequest().getURI().getPath();
            String query = exchange.getRequest().getURI().getQuery();
            return query != null ? "%s?%s".formatted(path, query) : path;
        }
        return "unknown";
    }

    /**
     * Restituisce il protocollo (schema) della richiesta.
     *
     * @param args array che contiene il {@link ServerWebExchange}
     * @return il protocollo (es. "http", "https") oppure "unknown"
     */
    @Override
    public String getProtocol(Object[] args) {
        if (args.length > 0 && args[0] instanceof ServerWebExchange exchange) {
            return exchange.getRequest().getURI().getScheme();
        }
        return "unknown";
    }

    /**
     * Restituisce l'indirizzo IP del client.
     *
     * @param args array che contiene il {@link ServerWebExchange}
     * @return l'indirizzo IP oppure "unknown" se non disponibile
     */
    @Override
    public String getClientIp(Object[] args) {
        if (args.length > 0 && args[0] instanceof ServerWebExchange exchange) {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (ip == null && exchange.getRequest().getRemoteAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return ip != null ? ip : "unknown";
        }
        return "unknown";
    }

    /**
     * Restituisce lo status HTTP della risposta.
     *
     * @param result il {@link ServerWebExchange} contenente la risposta
     * @return lo status code oppure "unknown" se non disponibile
     */
    @Override
    public String getStatus(Object result) {
        if (result instanceof ServerWebExchange exchange &&
                exchange.getResponse().getStatusCode() != null) {
            return String.valueOf(exchange.getResponse().getStatusCode().value());
        }
        return "unknown";
    }
}
