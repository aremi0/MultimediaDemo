package com.aremi.common.logging.conf;

import com.aremi.common.logging.extractor.MvcRequestExtractor;
import com.aremi.common.logging.extractor.RequestExtractor;
import com.aremi.common.logging.extractor.WebFluxRequestExtractor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configurazione che registra l'implementazione corretta di {@link RequestExtractor}
 * in base al tipo di applicazione (Servlet o WebFlux).
 *
 * <p>In un'applicazione MVC/Servlet viene registrato un {@link MvcRequestExtractor},
 * mentre in un'applicazione reattiva WebFlux viene registrato un
 * {@link WebFluxRequestExtractor}. In questo modo la libreria di logging
 * può funzionare in entrambi i contesti senza richiedere configurazioni manuali.</p>
 *
 * <p>L'annotazione {@code @ConditionalOnWebApplication} garantisce che venga
 * creato solo il bean appropriato al contesto corrente.</p>
 */

@Configuration
public class RequestExtractorAutoConfiguration {

    /**
     * Registra l'extractor per applicazioni MVC/Servlet.
     *
     * @return un'istanza di {@link MvcRequestExtractor}
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestExtractor mvcExtractor() {
        return new MvcRequestExtractor();
    }

    /**
     * Registra l'extractor per applicazioni WebFlux.
     *
     * @return un'istanza di {@link WebFluxRequestExtractor}
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public RequestExtractor webFluxExtractor() {
        return new WebFluxRequestExtractor();
    }
}
