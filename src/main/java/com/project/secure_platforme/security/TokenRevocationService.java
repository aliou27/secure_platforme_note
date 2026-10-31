package com.project.secure_platforme.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenRevocationService {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        purgeExpired();
        if (tokenId != null && expiresAt != null) {
            revokedTokens.put(tokenId, expiresAt);
        }
    }

    public boolean isRevoked(String tokenId) {
        return tokenId != null && revokedTokens.containsKey(tokenId);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        revokedTokens.values().removeIf(expiresAt -> expiresAt.isBefore(now));
    }
}
