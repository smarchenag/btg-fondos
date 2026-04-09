package com.btg.fondos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    private String fundId;
    private String fundName;
    private double amount;
    private LocalDateTime subscribedAt;
}
