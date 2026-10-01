package com.paymentwallet.wallet_service.controller;

import com.paymentwallet.wallet_service.dto.AddMoneyRequest;
import com.paymentwallet.wallet_service.dto.WalletResponse;
import com.paymentwallet.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(
            WalletService walletService
    ) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public WalletResponse getMyWallet(
            Authentication authentication
    ) {

        Long userId =
                (Long) authentication.getPrincipal();

        return walletService.getOrCreateWallet(userId);
    }

    @PostMapping("/add-money")
    public WalletResponse addMoney(
            Authentication authentication,
            @Valid @RequestBody AddMoneyRequest request
    ) {

        Long userId =
                (Long) authentication.getPrincipal();

        return walletService.addMoney(
                userId,
                request.amount()
        );
    }
}