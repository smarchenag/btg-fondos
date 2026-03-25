package com.btg.fondos.notification;

import com.btg.fondos.model.Client;

public interface NotificationService {
    void sendSubscriptionNotification(Client client, String fundName, double amount);
    void sendCancellationNotification(Client client, String fundName, double amount);
}
