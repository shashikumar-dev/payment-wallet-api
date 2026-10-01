package com.paymentwallet.wallet_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long transferId,
        Long senderWalletId,
        Long receiverWalletId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
}