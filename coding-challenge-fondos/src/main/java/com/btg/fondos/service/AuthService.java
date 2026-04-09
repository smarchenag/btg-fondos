package com.btg.fondos.service;

import com.btg.fondos.dto.AuthResponse;
import com.btg.fondos.dto.LoginRequest;
import com.btg.fondos.dto.RegisterRequest;
import com.btg.fondos.model.Client;
import com.btg.fondos.repository.ClientRepository;
import com.btg.fondos.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${app.initial-balance}")
    private double initialBalance;

    public AuthResponse register(RegisterRequest request) {
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Client client = Client.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .balance(initialBalance)
                .notificationPreference(request.getNotificationPreference())
                .build();
        clientRepository.save(client);

        String token = jwtTokenProvider.generateToken(client.getEmail(), client.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(client.getEmail())
                .name(client.getName())
                .role(client.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Client client = clientRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String token = jwtTokenProvider.generateToken(client.getEmail(), client.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(client.getEmail())
                .name(client.getName())
                .role(client.getRole().name())
                .build();
    }
}
