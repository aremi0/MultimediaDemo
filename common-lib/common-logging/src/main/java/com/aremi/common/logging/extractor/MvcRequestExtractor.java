package com.aremi.common.logging.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * Implementazione di {@link RequestExtractor} per applicazioni MVC/Servlet.
 *
 * <p>Estrae le informazioni della richiesta a partire da un
 * {@link jakarta.servlet.http.HttpServletRequest}, come metodo HTTP,
 * URI, protocollo, indirizzo IP del client e status della risposta.</p>
 *
 * <p>Gli argomenti del metodo del controller possono contenere
 * direttamente l'oggetto {@link HttpServletRequest}, che viene
 * ricercato e utilizzato per estrarre i dati.</p>
 */

public class MvcRequestExtractor implements RequestExtractor {

    /**
     * Restituisce il metodo HTTP della richiesta.
     *
     * @param args array di argomenti del metodo del controller
     * @return il metodo HTTP oppure "unknown" se non disponibile
     */
    @Override
    public String getMethod(Object[] args) {
        HttpServletRequest req = extract(args);
        return req != null ? req.getMethod() : "unknown";
    }

    /**
     * Restituisce l'URI della richiesta.
     *
     * @param args array di argomenti del metodo del controller
     * @return l'URI oppure "unknown" se non disponibile
     */
    @Override
    public String getUri(Object[] args) {
        HttpServletRequest req = extract(args);
        return req != null ? req.getRequestURI() : "unknown";
    }

    /**
     * Restituisce il protocollo della richiesta (es. HTTP/1.1).
     *
     * @param args array di argomenti del metodo del controller
     * @return il protocollo oppure "unknown" se non disponibile
     */
    @Override
    public String getProtocol(Object[] args) {
        HttpServletRequest req = extract(args);
        return req != null ? req.getProtocol() : "unknown";
    }

    /**
     * Restituisce l'indirizzo IP del client.
     *
     * <p>Se presente, viene usato l'header {@code X-Real-IP},
     * altrimenti l'indirizzo remoto della connessione.</p>
     *
     * @param args array di argomenti del metodo del controller
     * @return l'indirizzo IP oppure "unknown" se non disponibile
     */
    @Override
    public String getClientIp(Object[] args) {
        HttpServletRequest req = extract(args);
        if (req == null) return "unknown";
        String xfHeader = req.getHeader("X-Real-IP");
        return xfHeader != null ? xfHeader.split(",")[0].trim() : req.getRemoteAddr();
    }

    /**
     * Restituisce lo status HTTP della risposta.
     *
     * @param result il risultato del metodo del controller
     * @return lo status code oppure "unknown" se non disponibile
     */
    @Override
    public String getStatus(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            return String.valueOf(response.getStatusCode().value());
        }
        return "unknown";
    }

    /**
     * Cerca un {@link HttpServletRequest} tra gli argomenti del metodo.
     *
     * @param args array di argomenti del metodo del controller
     * @return l'oggetto {@link HttpServletRequest} se presente, altrimenti null
     */
    private HttpServletRequest extract(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest req) {
                return req;
            }
        }
        return null;
    }
}
