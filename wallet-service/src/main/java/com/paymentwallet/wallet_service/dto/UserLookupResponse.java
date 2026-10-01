package com.paymentwallet.wallet_service.dto;

public record UserLookupResponse(
        Long userId,
        String name,
        String walletPayId
) {
}