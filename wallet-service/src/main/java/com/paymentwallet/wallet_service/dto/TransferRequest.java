package com.paymentwallet.wallet_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(
                message = "Receiver WalletPay ID is required"
        )
        String receiverWalletPayId,

        @NotNull(
                message = "Amount is required"
        )
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount must have at most 2 decimal places"
        )
        BigDecimal amount

) {
}