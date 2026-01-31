package com.github.rrin.vulyk.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse {

    private LocalDateTime timestamp;

    private int status;

    private String error;

    private List<String> messages;

    public ErrorResponse(int status, String name, List<String> messages) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = name;
        this.messages = messages == null ? new ArrayList<>() : messages;
    }

    public ErrorResponse(int status, String name, String... messages) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = name;
        this.messages = messages == null ? new ArrayList<>() : List.of(messages);
    }
}
