package com.paymentwallet.wallet_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        String currency,
        Instant createdAt
) {
}