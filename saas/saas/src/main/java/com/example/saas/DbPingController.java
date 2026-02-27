package com.example.saas;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DbPingController {

    private final JdbcTemplate jdbcTemplate;

    public DbPingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ping-db")
    public Map<String, Object> pingDb() {
        String db = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Map.of("db", db, "ok", one);
    }
}