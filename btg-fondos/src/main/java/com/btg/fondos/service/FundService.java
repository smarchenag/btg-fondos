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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private final MongoTemplate mongoTemplate;

    public List<Fund> getAllFunds() {
        return fundRepository.findAll();
    }

    public Transaction subscribe(String clientId, String fundId) {
        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        Subscription newSubscription = Subscription.builder()
                .fundId(fund.getId())
                .fundName(fund.getName())
                .amount(fund.getMinimumAmount())
                .subscribedAt(LocalDateTime.now())
                .build();

        // Operación atómica: verifica condiciones y actualiza en un solo comando
        // Condición: el cliente existe, tiene saldo suficiente, y NO está suscrito al fondo
        Query query = new Query(Criteria.where("_id").is(clientId)
                .and("balance").gte(fund.getMinimumAmount())
                .and("subscriptions.fundId").ne(fundId));

        Update update = new Update()
                .inc("balance", -fund.getMinimumAmount())
                .push("subscriptions", newSubscription);

        Client updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Client.class
        );

        if (updated == null) {
            // El findAndModify no encontró documento que cumpla las condiciones.
            // Determinar la razón exacta para retornar el error apropiado.
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

            if (client.getSubscriptions().stream().anyMatch(s -> s.getFundId().equals(fundId))) {
                throw new AlreadySubscribedException(fund.getName());
            }
            throw new InsufficientBalanceException(fund.getName());
        }

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

        notificationService.sendSubscriptionNotification(updated, fund.getName(), fund.getMinimumAmount());

        return transaction;
    }

    public Transaction cancel(String clientId, String fundId) {
        Fund fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new FundNotFoundException(fundId));

        // Primero obtenemos la suscripción para conocer el monto a reintegrar
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Subscription subscription = client.getSubscriptions().stream()
                .filter(s -> s.getFundId().equals(fundId))
                .findFirst()
                .orElseThrow(() -> new NotSubscribedException(fund.getName()));

        // Operación atómica: elimina la suscripción y reintegra el saldo en un solo comando
        // Condición: el cliente existe y SÍ está suscrito al fondo
        Query query = new Query(Criteria.where("_id").is(clientId)
                .and("subscriptions.fundId").is(fundId));

        Update update = new Update()
                .inc("balance", subscription.getAmount())
                .pull("subscriptions", new Query(Criteria.where("fundId").is(fundId)));

        Client updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Client.class
        );

        if (updated == null) {
            // La suscripción fue cancelada por otra request concurrente
            throw new NotSubscribedException(fund.getName());
        }

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

        notificationService.sendCancellationNotification(updated, fund.getName(), subscription.getAmount());

        return transaction;
    }
}