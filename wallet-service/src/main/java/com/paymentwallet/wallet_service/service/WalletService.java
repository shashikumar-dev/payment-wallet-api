package com.paymentwallet.wallet_service.service;

import com.paymentwallet.wallet_service.dto.WalletResponse;
import com.paymentwallet.wallet_service.entity.Wallet;
import com.paymentwallet.wallet_service.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(
            WalletRepository walletRepository
    ) {
        this.walletRepository = walletRepository;
    }

    public WalletResponse getOrCreateWallet(Long userId) {

        Wallet wallet = walletRepository
                .findByUserId(userId)
                .orElseGet(() ->
                        walletRepository.save(
                                new Wallet(userId)
                        )
                );

        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse addMoney(
            Long userId,
            BigDecimal amount
    ) {

        Wallet wallet = walletRepository
                .findByUserId(userId)
                .orElseGet(() ->
                        walletRepository.save(
                                new Wallet(userId)
                        )
                );

        wallet.addMoney(amount);

        return toResponse(wallet);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                "INR",
                wallet.getCreatedAt()
        );
    }
}