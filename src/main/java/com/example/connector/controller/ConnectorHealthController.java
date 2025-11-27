package com.example.connector.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class ConnectorHealthController {

    @GetMapping(path = "/provision/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> body = Map.of(
                "status", "ok",
                "message", "Connector is reachable",
                "timestamp", Instant.now().toString());

        return ResponseEntity.ok(body);
    }

}
