package com.example.saas.dm;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class DmMessageSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner ensureDmMessageTable() {
        return args -> {
            jdbcTemplate.execute("""
                create table if not exists dm_messages (
                    id uuid primary key,
                    organization_id uuid not null,
                    channel varchar(32) not null,
                    sender_channel_id varchar(255),
                    sender_name varchar(255),
                    sender_phone varchar(64),
                    customer_hint varchar(255),
                    service_hint varchar(255),
                    message_text text not null,
                    intent varchar(32) not null default 'BOOK',
                    status varchar(32) not null,
                    customer_id uuid,
                    reservation_id uuid,
                    failure_reason text,
                    reply_text text,
                    parsed_start_at timestamptz,
                    parsed_end_at timestamptz,
                    received_at timestamptz not null,
                    processed_at timestamptz,
                    created_at timestamptz not null
                )
            """);

            jdbcTemplate.execute("alter table dm_messages add column if not exists parsed_start_at timestamptz");
            jdbcTemplate.execute("alter table dm_messages add column if not exists parsed_end_at timestamptz");
            jdbcTemplate.execute("alter table dm_messages add column if not exists reply_text text");
            jdbcTemplate.execute("alter table dm_messages add column if not exists intent varchar(32)");
            jdbcTemplate.execute("update dm_messages set intent = 'BOOK' where intent is null");
        };
    }
}
