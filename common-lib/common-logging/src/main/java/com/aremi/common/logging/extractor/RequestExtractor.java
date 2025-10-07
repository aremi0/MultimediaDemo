package com.aremi.common.logging.extractor;

/**
 * Interfaccia comune per l'estrazione delle informazioni di una richiesta HTTP.
 *
 * <p>Permette di astrarre le differenze tra contesti MVC (Servlet)
 * e WebFlux (reattivo), fornendo un contratto unico per ottenere
 * i dati principali della richiesta e della risposta.</p>
 *
 * <p>Le implementazioni concrete sono {@link MvcRequestExtractor}
 * e {@link WebFluxRequestExtractor}.</p>
 */

public interface RequestExtractor {
    /**
     * Restituisce il metodo HTTP della richiesta.
     *
     * @param args argomenti del metodo del controller o array contenente l'exchange
     * @return il metodo HTTP oppure "unknown"
     */
    String getMethod(Object[] args);

    /**
     * Restituisce l'URI della richiesta.
     *
     * @param args argomenti del metodo del controller o array contenente l'exchange
     * @return l'URI oppure "unknown"
     */
    String getUri(Object[] args);

    /**
     * Restituisce il protocollo della richiesta.
     *
     * @param args argomenti del metodo del controller o array contenente l'exchange
     * @return il protocollo oppure "unknown"
     */
    String getProtocol(Object[] args);

    /**
     * Restituisce l'indirizzo IP del client.
     *
     * @param args argomenti del metodo del controller o array contenente l'exchange
     * @return l'indirizzo IP oppure "unknown"
     */
    String getClientIp(Object[] args);

    /**
     * Restituisce lo status HTTP della risposta.
     *
     * @param result il risultato del metodo del controller o l'exchange
     * @return lo status code oppure "unknown"
     */
    String getStatus(Object result);
}
