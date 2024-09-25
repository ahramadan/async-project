package com.poc.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.model.CredentialsDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class AsyncService {

    private final WebClient webClient;
    private final Map<String, CompletableFuture<CredentialsDTO>> futureMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AsyncService(WebClient.Builder webClientBuilder,ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<CredentialsDTO> handleRequest(String input) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<CredentialsDTO> future = new CompletableFuture<>();
        futureMap.put(correlationId, future);

        // Asynchronously call the backend service
        webClient.post()
                .uri("http://localhost:8081/process")
                .header("Correlation-Id", correlationId)
                .bodyValue(input)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(ex -> {
                    futureMap.remove(correlationId);
                    future.completeExceptionally(ex);
                })
                .subscribe();

        // Return the future which will be completed when the Kafka message is received
        return future.orTimeout(30, TimeUnit.SECONDS)
                     .whenComplete((result, ex) -> futureMap.remove(correlationId));
    }

    @KafkaListener(topics = "response-topic")
    public void listen(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader("Correlation-Id");
        if (header != null) {
            String correlationId = new String(header.value(), StandardCharsets.UTF_8);
            CompletableFuture<CredentialsDTO> future = futureMap.get(correlationId);
            if (future != null) {
                try {
                    CredentialsDTO credentials = objectMapper.readValue(record.value(), CredentialsDTO.class);
                    future.complete(credentials);
                } catch (JsonProcessingException e) {
                    future.completeExceptionally(e);
                }
            }
        }
    }
}
