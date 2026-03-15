package com.example.saas.channel;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class ConnectedChannelSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner ensureConnectedChannelsTable() {
        return args -> {
            jdbcTemplate.execute("""
                create table if not exists connected_channels (
                    id uuid primary key,
                    organization_id uuid not null,
                    provider varchar(32) not null,
                    status varchar(32) not null,
                    external_account_id varchar(255),
                    account_name varchar(255),
                    username varchar(255),
                    access_token text,
                    refresh_token text,
                    token_expires_at timestamptz,
                    webhook_subscribed boolean not null default false,
                    oauth_state varchar(255),
                    connected_at timestamptz,
                    disconnected_at timestamptz,
                    created_at timestamptz not null,
                    updated_at timestamptz not null
                )
            """);

            jdbcTemplate.execute("alter table connected_channels add column if not exists oauth_state varchar(255)");
            jdbcTemplate.execute("alter table connected_channels add column if not exists webhook_subscribed boolean");
            jdbcTemplate.execute("alter table connected_channels add column if not exists account_name varchar(255)");
            jdbcTemplate.execute("alter table connected_channels add column if not exists username varchar(255)");
            jdbcTemplate.execute("alter table connected_channels add column if not exists access_token text");
            jdbcTemplate.execute("alter table connected_channels add column if not exists refresh_token text");
            jdbcTemplate.execute("alter table connected_channels add column if not exists token_expires_at timestamptz");
            jdbcTemplate.execute("alter table connected_channels add column if not exists connected_at timestamptz");
            jdbcTemplate.execute("alter table connected_channels add column if not exists disconnected_at timestamptz");
            jdbcTemplate.execute("alter table connected_channels add column if not exists created_at timestamptz");
            jdbcTemplate.execute("alter table connected_channels add column if not exists updated_at timestamptz");

            jdbcTemplate.execute("update connected_channels set webhook_subscribed = false where webhook_subscribed is null");
            jdbcTemplate.execute("update connected_channels set created_at = now() where created_at is null");
            jdbcTemplate.execute("update connected_channels set updated_at = now() where updated_at is null");

            jdbcTemplate.execute("alter table connected_channels alter column webhook_subscribed set default false");
            jdbcTemplate.execute("alter table connected_channels alter column webhook_subscribed set not null");
            jdbcTemplate.execute("alter table connected_channels alter column created_at set not null");
            jdbcTemplate.execute("alter table connected_channels alter column updated_at set not null");
        };
    }
}
