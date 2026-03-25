package com.btg.fondos.service;

import com.btg.fondos.model.Transaction;
import com.btg.fondos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getTransactionsByClient(String clientId) {
        return transactionRepository.findByClientIdOrderByTimestampDesc(clientId);
    }
}
