package com.paymentwallet.user_service.dto;

import com.paymentwallet.user_service.entity.UserStatus;

import java.time.Instant;

public record RegisterResponse(
        Long id,
        String name,
        String email,
        String walletPayId,
        UserStatus status,
        Instant createdAt
) {
}