package com.btg.fondos.dto;

import com.btg.fondos.model.NotificationPreference;
import com.btg.fondos.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ClientResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private double balance;
    private NotificationPreference notificationPreference;
    private List<Subscription> subscriptions;
}
