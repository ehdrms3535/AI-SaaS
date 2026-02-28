package com.example.saas.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String email, UUID orgId) { }