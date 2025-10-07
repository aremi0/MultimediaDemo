package com.aremi.common.logging.conf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

/**
 * Configurazione specifica per applicazioni WebFlux.
 *
 * <p>Registra un {@link org.springframework.web.server.WebFilter} che intercetta ogni richiesta
 * e inserisce l'oggetto {@link org.springframework.web.server.ServerWebExchange}
 * all'interno del Reactor Context. In questo modo l'aspect di logging può recuperare
 * l'exchange in maniera non bloccante e senza doverlo passare esplicitamente
 * come argomento del controller.</p>
 *
 * <p>Questa configurazione viene caricata solo in applicazioni di tipo REACTIVE
 * grazie all'annotazione {@code @ConditionalOnWebApplication(type = REACTIVE)}.</p>
 */

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ExchangeContextConfiguration {

    /**
     * Filtro WebFlux che inserisce il {@link ServerWebExchange} nel Reactor Context.
     *
     * @return il filtro registrato nella catena di WebFlux
     */
    @Bean
    public WebFilter exchangeInjectorFilter() {
        return (exchange, chain) ->
                chain.filter(exchange)
                        .contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange));
    }
}
