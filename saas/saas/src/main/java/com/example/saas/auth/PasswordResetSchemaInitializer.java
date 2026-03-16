package com.example.saas.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class PasswordResetSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner ensurePasswordResetTokensTable() {
        return args -> {
            jdbcTemplate.execute("""
                create table if not exists password_reset_tokens (
                    id uuid primary key,
                    user_id uuid not null,
                    token_hash varchar(64) not null unique,
                    expires_at timestamptz not null,
                    used_at timestamptz,
                    created_at timestamptz not null
                )
            """);

            jdbcTemplate.execute("alter table password_reset_tokens add column if not exists user_id uuid");
            jdbcTemplate.execute("alter table password_reset_tokens add column if not exists token_hash varchar(64)");
            jdbcTemplate.execute("alter table password_reset_tokens add column if not exists expires_at timestamptz");
            jdbcTemplate.execute("alter table password_reset_tokens add column if not exists used_at timestamptz");
            jdbcTemplate.execute("alter table password_reset_tokens add column if not exists created_at timestamptz");

            jdbcTemplate.execute("create unique index if not exists idx_password_reset_tokens_token_hash on password_reset_tokens(token_hash)");
            jdbcTemplate.execute("create index if not exists idx_password_reset_tokens_user_id on password_reset_tokens(user_id)");
            jdbcTemplate.execute("update password_reset_tokens set created_at = now() where created_at is null");
            jdbcTemplate.execute("alter table password_reset_tokens alter column created_at set not null");
        };
    }
}
