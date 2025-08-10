package com.aremi.kafkaproducerservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import requestlog.RequestLogOuterClass;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RequestLogReceiverService extends requestlog.RequestLogReceiverGrpc.RequestLogReceiverImplBase {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka-topic.request.default:log.request.default}")
    private String defaultRequestTopic;

    @Override
    public void sendLog(RequestLogOuterClass.RequestLog request, StreamObserver<RequestLogOuterClass.Ack> responseObserver) {
        try {
            // Chiave = IP client (o altro campo utile al partitioning)
            var key = StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : "unknown";

            // Serializziamo in JSON per semplicità di consumo lato Kafka
            var requestLogPojo = toPojo(request);
            var topic = requestLogPojo.kafka_topic();
            var json = objectMapper.writeValueAsString(requestLogPojo);

            if (!StringUtils.hasText(topic)) {
                log.warn("[WARNING] kafka-topic non specificato, impostando il default: '{}'", defaultRequestTopic);
                topic = defaultRequestTopic;
            }

            kafkaTemplate.send(topic, key, json);

            RequestLogOuterClass.Ack ack = RequestLogOuterClass.Ack.newBuilder()
                    .setStatus(1)
                    .setMessage("OK")
                    .build();
            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            RequestLogOuterClass.Ack ack = RequestLogOuterClass.Ack.newBuilder()
                    .setStatus(0)
                    .setMessage("ERROR: " + ex.getLocalizedMessage())
                    .build();
            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        }
    }

    private RequestLogPojo toPojo(RequestLogOuterClass.RequestLog log) {
        return new RequestLogPojo(
                log.getTime(),
                log.getRemoteAddr(),
                log.getRequest(),
                log.getStatus(),
                log.getRequestTime(),
                log.getService(),
                log.getKafkaTopic()
        );
    }

    public record RequestLogPojo(
            String time,
            String remote_addr,
            String request,
            String status,
            String request_time,
            String service,
            String kafka_topic
    ) {}
}
