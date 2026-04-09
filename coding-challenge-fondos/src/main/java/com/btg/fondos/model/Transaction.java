package com.btg.fondos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;
    private String clientId;
    private String fundId;
    private String fundName;
    private TransactionType type;
    private double amount;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
