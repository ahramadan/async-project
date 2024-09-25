package com.poc.controller;

import com.poc.model.CredentialsDTO;
import com.poc.service.AsyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class AsyncController {

    private final AsyncService aService;

    public AsyncController(AsyncService aService) {
        this.aService = aService;
    }

    @GetMapping("/process")
    public DeferredResult<ResponseEntity<CredentialsDTO>> processRequest(@RequestParam String input) {
        DeferredResult<ResponseEntity<CredentialsDTO>> deferredResult = new DeferredResult<>(30_000L);

        deferredResult.onTimeout(() -> {
            deferredResult.setErrorResult(
                ResponseEntity.status(504).body("Request timed out.")
            );
        });

        CompletableFuture<CredentialsDTO> futureResponse = aService.handleRequest(input);

        futureResponse.thenAccept(response -> {
            deferredResult.setResult(ResponseEntity.ok(response));
        }).exceptionally(ex -> {
            deferredResult.setErrorResult(
                ResponseEntity.status(500).body("An error occurred: " + ex.getMessage())
            );
            return null;
        });

        return deferredResult;
    }
}
