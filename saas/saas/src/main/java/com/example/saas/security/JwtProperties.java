package com.example.saas.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String issuer;
    private int accessTokenMinutes;
    private int refreshTokenDays;
    private String secret;
}