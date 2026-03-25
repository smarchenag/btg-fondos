package com.btg.fondos.service;

import com.btg.fondos.exception.AlreadySubscribedException;
import com.btg.fondos.exception.FundNotFoundException;
import com.btg.fondos.exception.InsufficientBalanceException;
import com.btg.fondos.exception.NotSubscribedException;
import com.btg.fondos.model.*;
import com.btg.fondos.notification.NotificationService;
import com.btg.fondos.repository.ClientRepository;
import com.btg.fondos.repository.FundRepository;
import com.btg.fondos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FundService {

    private final FundRepository fundRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public List<Fund> getAllFunds() {
        return fundRepository.findAll();
    }

    public Transaction subscribe(String clientId, String fundId) {
        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        boolean alreadySubscribed = client.getSubscriptions().stream()
                .anyMatch(s -> s.getFundId().equals(fundId));
        if (alreadySubscribed) {
            throw new AlreadySubscribedException(fund.getName());
        }

        if (client.getBalance() < fund.getMinimumAmount()) {
            throw new InsufficientBalanceException(fund.getName());
        }

        client.setBalance(client.getBalance() - fund.getMinimumAmount());
        client.getSubscriptions().add(Subscription.builder()
                .fundId(fund.getId())
                .fundName(fund.getName())
                .amount(fund.getMinimumAmount())
                .subscribedAt(LocalDateTime.now())
                .build());
        clientRepository.save(client);

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .clientId(clientId)
                .fundId(fund.getId())
                .fundName(fund.getName())
                .type(TransactionType.SUBSCRIBE)
                .amount(fund.getMinimumAmount())
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        notificationService.sendSubscriptionNotification(client, fund.getName(), fund.getMinimumAmount());

        return transaction;
    }

    public Transaction cancel(String clientId, String fundId) {
        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Subscription subscription = client.getSubscriptions().stream()
                .filter(s -> s.getFundId().equals(fundId))
                .findFirst()
                .orElseThrow(() -> new NotSubscribedException(fund.getName()));

        client.setBalance(client.getBalance() + subscription.getAmount());
        client.getSubscriptions().removeIf(s -> s.getFundId().equals(fundId));
        clientRepository.save(client);

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .clientId(clientId)
                .fundId(fund.getId())
                .fundName(fund.getName())
                .type(TransactionType.CANCEL)
                .amount(subscription.getAmount())
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        notificationService.sendCancellationNotification(client, fund.getName(), subscription.getAmount());

        return transaction;
    }
}