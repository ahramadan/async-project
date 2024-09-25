package com.poc.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CredentialsDTO {
    private String correlationId;
    private String username;
    private String password;
}
