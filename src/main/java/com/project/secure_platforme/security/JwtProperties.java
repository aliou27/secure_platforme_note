package com.project.secure_platforme.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank
        String secret,

        @NotBlank
        String issuer,

        @NotNull
        Duration expiration
) {
    @Override
    public String toString() {
        return "JwtProperties{issuer='" + issuer + "', expiration=" + expiration + "}";
    }
}
