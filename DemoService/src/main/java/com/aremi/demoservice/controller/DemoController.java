package com.aremi.demoservice.controller;

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

    @GetMapping("/demo")
    public ResponseEntity<String> demo() {
        return ResponseEntity.ok("Hello World");
    }
}
