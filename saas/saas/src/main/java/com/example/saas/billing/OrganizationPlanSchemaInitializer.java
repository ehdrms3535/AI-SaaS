package com.example.saas.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class OrganizationPlanSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner ensureOrganizationPlanColumn() {
        return args -> {
            jdbcTemplate.execute("alter table organizations add column if not exists plan varchar(32)");
            jdbcTemplate.execute("alter table organizations add column if not exists business_open_time time");
            jdbcTemplate.execute("alter table organizations add column if not exists business_close_time time");
            jdbcTemplate.execute("alter table organizations add column if not exists closed_weekdays varchar(128)");
            jdbcTemplate.execute("alter table organizations add column if not exists dm_webhook_enabled boolean");
            jdbcTemplate.execute("alter table organizations add column if not exists dm_webhook_secret varchar(255)");
            jdbcTemplate.update("update organizations set plan = ? where plan is null", OrganizationPlan.FREE.name());
            jdbcTemplate.execute("update organizations set business_open_time = '09:00:00' where business_open_time is null");
            jdbcTemplate.execute("update organizations set business_close_time = '21:00:00' where business_close_time is null");
            jdbcTemplate.update("update organizations set closed_weekdays = ? where closed_weekdays is null or closed_weekdays = ''", "SUNDAY");
            jdbcTemplate.execute("update organizations set dm_webhook_enabled = false where dm_webhook_enabled is null");
            jdbcTemplate.execute("alter table organizations alter column plan set default 'FREE'");
            jdbcTemplate.execute("alter table organizations alter column plan set not null");
            jdbcTemplate.execute("alter table organizations alter column business_open_time set default '09:00:00'");
            jdbcTemplate.execute("alter table organizations alter column business_open_time set not null");
            jdbcTemplate.execute("alter table organizations alter column business_close_time set default '21:00:00'");
            jdbcTemplate.execute("alter table organizations alter column business_close_time set not null");
            jdbcTemplate.execute("alter table organizations alter column closed_weekdays set default 'SUNDAY'");
            jdbcTemplate.execute("alter table organizations alter column closed_weekdays set not null");
            jdbcTemplate.execute("alter table organizations alter column dm_webhook_enabled set default false");
            jdbcTemplate.execute("alter table organizations alter column dm_webhook_enabled set not null");
        };
    }
}
