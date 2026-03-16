package com.example.saas.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private PasswordReset passwordReset = new PasswordReset();

    @Getter
    @Setter
    public static class PasswordReset {
        private String mailFrom;
    }
}
