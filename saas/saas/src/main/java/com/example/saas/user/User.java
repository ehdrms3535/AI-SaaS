package com.example.saas.user;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    // 반드시 존재해야 함 (BCrypt 해시)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;
}