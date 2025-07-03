package com.aremi.demoservice.controller;

import com.aremi.demoservice.kafka.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Classe che gestisce gli Endpoint legati all'autenticazione
 */

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class DemoController {

    private final KafkaProducer kafkaProducer;

    @GetMapping("/demo")
    public ResponseEntity<String> demo() {
        log.info("Richiesta arrivata");
        kafkaProducer.sendLog("demoController", "GET /demo Hello World");
        return ResponseEntity.ok("Hello World");
    }
}
