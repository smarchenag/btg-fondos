package com.btg.fondos.notification;

import com.btg.fondos.model.Client;
import com.btg.fondos.model.NotificationPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final SesClient sesClient;
    private final boolean sesEnabled;
    private final String senderEmail;

    public NotificationServiceImpl(
            @Nullable SesClient sesClient,
            @Value("${app.ses.enabled:false}") boolean sesEnabled,
            @Value("${app.ses.sender-email:noreply@btgfondos.com}") String senderEmail) {
        this.sesClient = sesClient;
        this.sesEnabled = sesEnabled;
        this.senderEmail = senderEmail;
    }

    @Override
    public void sendSubscriptionNotification(Client client, String fundName, double amount) {
        String subject = "Suscripción exitosa - " + fundName;
        String message = String.format(
                "Estimado(a) %s,\n\n"
                + "Se ha suscrito exitosamente al fondo %s por un monto de COP $%,.0f.\n\n"
                + "Su saldo disponible es: COP $%,.0f.\n\n"
                + "Gracias por confiar en BTG Pactual.",
                client.getName(), fundName, amount, client.getBalance()
        );
        send(client, subject, message);
    }

    @Override
    public void sendCancellationNotification(Client client, String fundName, double amount) {
        String subject = "Cancelación de suscripción - " + fundName;
        String message = String.format(
                "Estimado(a) %s,\n\n"
                + "Se ha cancelado su suscripción al fondo %s. "
                + "Se han reintegrado COP $%,.0f a su saldo.\n\n"
                + "Su saldo disponible es: COP $%,.0f.\n\n"
                + "Gracias por confiar en BTG Pactual.",
                client.getName(), fundName, amount, client.getBalance()
        );
        send(client, subject, message);
    }

    private void send(Client client, String subject, String message) {
        if (client.getNotificationPreference() == NotificationPreference.EMAIL) {
            sendEmail(client.getEmail(), subject, message);
        } else {
            sendSms(client.getPhone(), message);
        }
    }

    private void sendEmail(String recipientEmail, String subject, String message) {
        if (sesEnabled && sesClient != null) {
            try {
                SendEmailRequest request = SendEmailRequest.builder()
                        .source(senderEmail)
                        .destination(Destination.builder()
                                .toAddresses(recipientEmail)
                                .build())
                        .message(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(message).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build();
                sesClient.sendEmail(request);
                log.info("[EMAIL ENVIADO -> {}] {}", recipientEmail, subject);
            } catch (SesException e) {
                log.error("[EMAIL ERROR -> {}] {}: {}", recipientEmail, subject, e.getMessage());
            }
        } else {
            log.info("[EMAIL SIMULADO -> {}] {} | {}", recipientEmail, subject, message);
        }
    }

    private void sendSms(String phone, String message) {
        // SMS via AWS SNS se puede agregar en el futuro
        log.info("[SMS SIMULADO -> {}] {}", phone, message);
    }
}
