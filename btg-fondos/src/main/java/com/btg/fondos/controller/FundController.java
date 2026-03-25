package com.btg.fondos.controller;

import com.btg.fondos.model.Client;
import com.btg.fondos.model.Fund;
import com.btg.fondos.model.Transaction;
import com.btg.fondos.repository.ClientRepository;
import com.btg.fondos.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;
    private final ClientRepository clientRepository;

    @GetMapping
    public ResponseEntity<List<Fund>> getAllFunds() {
        return ResponseEntity.ok(fundService.getAllFunds());
    }

    @PostMapping("/{fundId}/subscribe")
    public ResponseEntity<Transaction> subscribe(
            @PathVariable String fundId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Client client = getClient(userDetails);
        return ResponseEntity.ok(fundService.subscribe(client.getId(), fundId));
    }

    @PostMapping("/{fundId}/cancel")
    public ResponseEntity<Transaction> cancel(
            @PathVariable String fundId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Client client = getClient(userDetails);
        return ResponseEntity.ok(fundService.cancel(client.getId(), fundId));
    }

    private Client getClient(UserDetails userDetails) {
        return clientRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }
}
