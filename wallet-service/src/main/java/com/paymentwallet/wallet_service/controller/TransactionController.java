package com.paymentwallet.wallet_service.controller;

import com.paymentwallet.wallet_service.dto.PagedTransactionResponse;
import com.paymentwallet.wallet_service.dto.TransactionResponse;
import com.paymentwallet.wallet_service.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService
    ) {
        this.transactionService =
                transactionService;
    }

    @GetMapping
    public PagedTransactionResponse getTransactions(
            Authentication authentication,

            @RequestHeader("Authorization")
            String authorizationHeader,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size
    ) {

        Long userId =
                (Long) authentication
                        .getPrincipal();

        return transactionService
                .getTransactions(
                        userId,
                        authorizationHeader,
                        page,
                        size
                );
    }

    @GetMapping("/{transferId}")
    public TransactionResponse getTransaction(
            Authentication authentication,

            @RequestHeader("Authorization")
            String authorizationHeader,

            @PathVariable
            Long transferId
    ) {

        Long userId =
                (Long) authentication
                        .getPrincipal();

        return transactionService
                .getTransaction(
                        userId,
                        transferId,
                        authorizationHeader
                );
    }
}