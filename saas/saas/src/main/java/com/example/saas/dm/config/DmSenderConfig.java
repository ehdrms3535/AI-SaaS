package com.example.saas.dm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DmSenderProperties.class)
public class DmSenderConfig {

    @Bean
    RestClient dmRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
