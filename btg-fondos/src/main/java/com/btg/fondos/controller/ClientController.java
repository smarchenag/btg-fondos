package com.btg.fondos.controller;

import com.btg.fondos.dto.ClientResponse;
import com.btg.fondos.model.Client;
import com.btg.fondos.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;

    @GetMapping("/me")
    public ResponseEntity<ClientResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Client client = clientRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        ClientResponse response = ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .balance(client.getBalance())
                .notificationPreference(client.getNotificationPreference())
                .subscriptions(client.getSubscriptions())
                .build();

        return ResponseEntity.ok(response);
    }
}
