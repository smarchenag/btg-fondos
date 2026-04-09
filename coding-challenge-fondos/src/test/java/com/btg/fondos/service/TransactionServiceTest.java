package com.btg.fondos.service;

import com.btg.fondos.model.Transaction;
import com.btg.fondos.model.TransactionType;
import com.btg.fondos.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Obtener historial de transacciones del cliente")
    void getTransactionsByClient() {
        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .id("tx-1").clientId("c1").fundId("1").fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                        .type(TransactionType.SUBSCRIBE).amount(75_000).timestamp(LocalDateTime.now())
                        .build(),
                Transaction.builder()
                        .id("tx-2").clientId("c1").fundId("1").fundName("FPV_BTG_PACTUAL_RECAUDADORA")
                        .type(TransactionType.CANCEL).amount(75_000).timestamp(LocalDateTime.now())
                        .build()
        );

        when(transactionRepository.findByClientIdOrderByTimestampDesc("c1")).thenReturn(transactions);

        List<Transaction> result = transactionService.getTransactionsByClient("c1");

        assertEquals(2, result.size());
        assertEquals(TransactionType.SUBSCRIBE, result.get(0).getType());
        assertEquals(TransactionType.CANCEL, result.get(1).getType());
    }

    @Test
    @DisplayName("Retorna lista vacía si no hay transacciones")
    void getTransactionsByClient_empty() {
        when(transactionRepository.findByClientIdOrderByTimestampDesc("c2")).thenReturn(List.of());

        List<Transaction> result = transactionService.getTransactionsByClient("c2");

        assertEquals(0, result.size());
    }
}
