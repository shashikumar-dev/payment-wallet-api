package com.paymentwallet.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "WalletPay ID is required")
        @Size(
                min = 3,
                max = 30,
                message = "WalletPay ID must be between 3 and 30 characters"
        )
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "WalletPay ID can contain only letters, numbers, dot, underscore and hyphen"
        )
        String walletPayId,


        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72,
                message = "Password must be between 8 and 72 characters")
        String password

) {
}