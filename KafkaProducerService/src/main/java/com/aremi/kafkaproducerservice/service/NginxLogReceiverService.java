package com.aremi.kafkaproducerservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import nginxlog.NginxLogOuterClass;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

@GrpcService
@RequiredArgsConstructor
public class NginxLogReceiverService extends nginxlog.NginxLogReceiverGrpc.NginxLogReceiverImplBase {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic:nginx.logs}")
    private String topic;

    @Override
    public void sendLog(NginxLogOuterClass.NginxLog request, StreamObserver<NginxLogOuterClass.Ack> responseObserver) {
        try {
            // Chiave = IP client (o altro campo utile al partitioning)
            var key = StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : null;

            // Serializziamo in JSON per semplicità di consumo lato Kafka
            var json = objectMapper.writeValueAsString(toPojo(request));
            kafkaTemplate.send(topic, key, json);

            NginxLogOuterClass.Ack ack = NginxLogOuterClass.Ack.newBuilder().setMessage("OK").build();
            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            NginxLogOuterClass.Ack ack = NginxLogOuterClass.Ack.newBuilder().setMessage("ERROR: " + ex.getLocalizedMessage()).build();
            responseObserver.onNext(ack);
            responseObserver.onCompleted();
        }
    }

    private NginxLogPojo toPojo(NginxLogOuterClass.NginxLog log) {
        return new NginxLogPojo(
                log.getTime(),
                log.getRemoteAddr(),
                log.getRequest(),
                log.getStatus(),
                log.getHttpUserAgent(),
                log.getRequestTime(),
                log.getUpstreamResponseTime()
        );
    }

    public record NginxLogPojo(
            String time,
            String remote_addr,
            String request,
            String status,
            String http_user_agent,
            String request_time,
            String upstream_response_time
    ) {}
}
