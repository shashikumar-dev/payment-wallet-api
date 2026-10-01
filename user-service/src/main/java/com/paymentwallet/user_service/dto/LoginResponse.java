package com.paymentwallet.user_service.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}