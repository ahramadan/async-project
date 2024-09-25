package com.poc.controller;

import com.poc.model.CredentialsDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/process")
public class KafkaBackendController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaBackendController(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public void processRequest(@RequestHeader("Correlation-Id") String correlationId, @RequestBody String input) {
        CredentialsDTO credentials = new CredentialsDTO(
                correlationId,
                "user_" + input,
                "pass_" + input
        );

        // Convert CredentialsDTO to JSON string
        String jsonResponse;
        try {
            jsonResponse = objectMapper.writeValueAsString(credentials);
        } catch (JsonProcessingException e) {
            // Handle exception
            throw new RuntimeException("Error converting CredentialsDTO to JSON", e);
        }

        // Publish to Kafka
        ProducerRecord<String, String> record = new ProducerRecord<>("response-topic", jsonResponse);
        record.headers().add(new RecordHeader("Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8)));

        // Send the record to Kafka
        kafkaTemplate.send(record);
    }
}
