package com.aremi.apigateway.conf;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import requestlog.RequestLogReceiverGrpc;

@Configuration
public class RequestLogConfigurer {
    @Value("${app.kafka-producer.host:spring-kafka-producer}")
    private String host;

    @Value("${app.kafka-producer.port:6565}")
    private int port;

    @Bean
    public RequestLogReceiverGrpc.RequestLogReceiverBlockingStub requestLogReceiverBlockingStub() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        return RequestLogReceiverGrpc.newBlockingStub(channel);
    }
}
