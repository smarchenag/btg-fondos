package com.btg.fondos.controller;

import com.btg.fondos.model.Client;
import com.btg.fondos.model.Transaction;
import com.btg.fondos.repository.ClientRepository;
import com.btg.fondos.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final ClientRepository clientRepository;

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Client client = clientRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        return ResponseEntity.ok(transactionService.getTransactionsByClient(client.getId()));
    }
}
