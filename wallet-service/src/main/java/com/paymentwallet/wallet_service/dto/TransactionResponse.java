package com.paymentwallet.wallet_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long transferId,
        BigDecimal amount,
        String type,
        String status,
        Instant createdAt,
        CounterpartyResponse counterparty
) {
}