package com.paymentwallet.user_service.dto;

public record UserLookupResponse(
        Long userId,
        String name,
        String walletPayId
) {
}