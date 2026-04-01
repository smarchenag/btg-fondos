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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundServiceTest {

    @Mock private FundRepository fundRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationService notificationService;
    @Mock private MongoTemplate mongoTemplate;

    @InjectMocks
    private FundService fundService;

    private Fund fund;
    private Client client;
    private Client clientAfterSubscribe;
    private Client clientAfterCancel;

    @BeforeEach
    void setUp() {
        fund = Fund.builder()
                .id("1")
                .name("FPV_BTG_PACTUAL_RECAUDADORA")
                .minimumAmount(75_000)
                .category("FPV")
                .build();

        client = Client.builder()
                .id("client-1")
                .name("Juan Pérez")
                .email("juan@test.com")
                .phone("+57300000000")
                .balance(500_000)
                .notificationPreference(NotificationPreference.EMAIL)
                .subscriptions(new ArrayList<>())
                .build();

        // Estado del cliente después de una suscripción exitosa
        clientAfterSubscribe = Client.builder()
                .id("client-1")
                .name("Juan Pérez")
                .email("juan@test.com")
                .balance(425_000)
                .subscriptions(new ArrayList<>(List.of(
                        Subscription.builder().fundId("1").fundName(fund.getName())
                                .amount(75_000).subscribedAt(LocalDateTime.now()).build())))
                .build();

        // Estado del cliente después de una cancelación exitosa
        clientAfterCancel = Client.builder()
                .id("client-1")
                .name("Juan Pérez")
                .email("juan@test.com")
                .balance(500_000)
                .subscriptions(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Suscripción exitosa: findAndModify retorna cliente actualizado")
    void subscribe_success() {
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Client.class)))
                .thenReturn(clientAfterSubscribe);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = fundService.subscribe("client-1", "1");

        assertEquals(TransactionType.SUBSCRIBE, result.getType());
        assertEquals(75_000, result.getAmount());
        assertEquals("1", result.getFundId());
        verify(notificationService).sendSubscriptionNotification(clientAfterSubscribe, fund.getName(), 75_000);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Suscripción falla por saldo insuficiente: findAndModify retorna null")
    void subscribe_insufficientBalance() {
        client.setBalance(50_000);
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        // findAndModify retorna null porque la condición de saldo no se cumple
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Client.class)))
                .thenReturn(null);
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client));

        InsufficientBalanceException ex = assertThrows(
                InsufficientBalanceException.class,
                () -> fundService.subscribe("client-1", "1")
        );

        assertTrue(ex.getMessage().contains("FPV_BTG_PACTUAL_RECAUDADORA"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Suscripción falla si ya está suscrito: findAndModify retorna null")
    void subscribe_alreadySubscribed() {
        client.getSubscriptions().add(Subscription.builder()
                .fundId("1").fundName(fund.getName()).amount(75_000).subscribedAt(LocalDateTime.now()).build());

        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Client.class)))
                .thenReturn(null);
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client));

        assertThrows(AlreadySubscribedException.class, () -> fundService.subscribe("client-1", "1"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Suscripción falla si el fondo no existe")
    void subscribe_fundNotFound() {
        when(fundRepository.findById("99")).thenReturn(Optional.empty());

        assertThrows(FundNotFoundException.class, () -> fundService.subscribe("client-1", "99"));
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Client.class));
    }

    @Test
    @DisplayName("Cancelación exitosa: findAndModify retorna cliente con saldo reintegrado")
    void cancel_success() {
        client.setBalance(425_000);
        client.getSubscriptions().add(Subscription.builder()
                .fundId("1").fundName(fund.getName()).amount(75_000).subscribedAt(LocalDateTime.now()).build());

        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Client.class)))
                .thenReturn(clientAfterCancel);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = fundService.cancel("client-1", "1");

        assertEquals(TransactionType.CANCEL, result.getType());
        assertEquals(75_000, result.getAmount());
        verify(notificationService).sendCancellationNotification(clientAfterCancel, fund.getName(), 75_000);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Cancelación falla si no está suscrito")
    void cancel_notSubscribed() {
        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client));

        assertThrows(NotSubscribedException.class, () -> fundService.cancel("client-1", "1"));
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Client.class));
    }

    @Test
    @DisplayName("Listar todos los fondos")
    void getAllFunds() {
        List<Fund> funds = List.of(fund);
        when(fundRepository.findAll()).thenReturn(funds);

        List<Fund> result = fundService.getAllFunds();

        assertEquals(1, result.size());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", result.get(0).getName());
    }

    @Test
    @DisplayName("Múltiples suscripciones: cada findAndModify es atómico e independiente")
    void subscribe_multiple_funds() {
        Fund fund2 = Fund.builder().id("3").name("DEUDAPRIVADA").minimumAmount(50_000).category("FIC").build();

        Client afterFirst = Client.builder().id("client-1").balance(425_000)
                .subscriptions(new ArrayList<>(List.of(
                        Subscription.builder().fundId("1").fundName(fund.getName())
                                .amount(75_000).subscribedAt(LocalDateTime.now()).build())))
                .build();

        Client afterSecond = Client.builder().id("client-1").balance(375_000)
                .subscriptions(new ArrayList<>(List.of(
                        Subscription.builder().fundId("1").fundName(fund.getName())
                                .amount(75_000).subscribedAt(LocalDateTime.now()).build(),
                        Subscription.builder().fundId("3").fundName(fund2.getName())
                                .amount(50_000).subscribedAt(LocalDateTime.now()).build())))
                .build();

        when(fundRepository.findById("1")).thenReturn(Optional.of(fund));
        when(fundRepository.findById("3")).thenReturn(Optional.of(fund2));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Client.class)))
                .thenReturn(afterFirst)
                .thenReturn(afterSecond);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        fundService.subscribe("client-1", "1");
        fundService.subscribe("client-1", "3");

        verify(mongoTemplate, times(2)).findAndModify(any(), any(), any(), eq(Client.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }
}
