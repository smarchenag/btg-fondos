package com.btg.fondos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.annotation.Version;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "clients")
public class Client {

    @Id
    private String id;

    @Version
    private Long version;
    private String name;

    @Indexed(unique = true)
    private String email;

    private String phone;
    private String password;

    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private double balance = 500_000;

    @Builder.Default
    private NotificationPreference notificationPreference = NotificationPreference.EMAIL;

    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();
}
