package com.btg.fondos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConfig {

    @Value("${app.ses.enabled:false}")
    private boolean sesEnabled;

    @Value("${app.ses.region:us-east-1}")
    private String sesRegion;

    @Bean
    public SesClient sesClient() {
        if (!sesEnabled) {
            return null;
        }
        return SesClient.builder()
                .region(Region.of(sesRegion))
                .build();
    }
}
