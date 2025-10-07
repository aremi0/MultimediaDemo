package com.aremi.common.logging.conf;

import com.aremi.common.logging.extractor.MvcRequestExtractor;
import com.aremi.common.logging.extractor.RequestExtractor;
import com.aremi.common.logging.extractor.WebFluxRequestExtractor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestExtractorAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestExtractor mvcExtractor() {
        return new MvcRequestExtractor();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public RequestExtractor webFluxExtractor() {
        return new WebFluxRequestExtractor();
    }
}
