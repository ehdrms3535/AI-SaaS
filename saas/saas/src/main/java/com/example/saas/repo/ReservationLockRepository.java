package com.example.saas.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

@Repository
@RequiredArgsConstructor
public class ReservationLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public void lockTx(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("lock key is blank");
        }

        jdbcTemplate.execute(
                "SELECT pg_advisory_xact_lock(hashtext(?))",
                (PreparedStatement ps) -> {
                    ps.setString(1, key);
                    ps.execute();   // ✅ 결과 매핑 안 함
                    return null;
                }
        );
    }
}