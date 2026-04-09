package com.btg.fondos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ApiError {
    private int status;
    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
