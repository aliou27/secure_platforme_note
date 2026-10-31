package com.project.secure_platforme.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    @Override
    public String toString() {
        return "TokenResponse{tokenType='" + tokenType + "', expiresIn=" + expiresIn + "}";
    }
}
