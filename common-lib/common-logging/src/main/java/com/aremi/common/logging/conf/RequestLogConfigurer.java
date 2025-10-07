package com.aremi.common.logging.conf;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import requestlog.RequestLogReceiverGrpc;

/**
 * Configurazione responsabile della creazione dello stub gRPC
 * per l'invio dei log delle richieste a un servizio esterno.
 *
 * <p>Utilizza i parametri {@code app.kafka-producer.host} e
 * {@code app.kafka-producer.port} per connettersi al servizio gRPC
 * che riceve i log. Se non specificati, vengono usati i valori
 * di default {@code spring-kafka-producer:6565}.</p>
 *
 * <p>Il bean esposto è un {@link RequestLogReceiverGrpc.RequestLogReceiverBlockingStub}
 * che viene iniettato nell'aspect di logging per inviare i log
 * in maniera sincrona.</p>
 */

@Configuration
public class RequestLogConfigurer {

    @Value("${app.kafka-producer.host:spring-kafka-producer}")
    private String host;

    @Value("${app.kafka-producer.port:6565}")
    private int port;

    /**
     * Crea lo stub gRPC per comunicare con il servizio di ricezione log.
     *
     * @return lo stub gRPC configurato
     */
    @Bean
    public RequestLogReceiverGrpc.RequestLogReceiverBlockingStub requestLogReceiverBlockingStub() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        return RequestLogReceiverGrpc.newBlockingStub(channel);
    }
}
