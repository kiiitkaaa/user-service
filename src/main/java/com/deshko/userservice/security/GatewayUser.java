package com.deshko.userservice.security;

import org.springframework.security.core.AuthenticatedPrincipal;

public record GatewayUser(Long id, String email, String role) implements AuthenticatedPrincipal {
    @Override
    public String getName() {
        return id.toString();
    }
}