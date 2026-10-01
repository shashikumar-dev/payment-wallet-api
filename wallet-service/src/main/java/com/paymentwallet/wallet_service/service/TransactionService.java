package com.paymentwallet.wallet_service.service;

import com.paymentwallet.wallet_service.client.UserServiceClient;
import com.paymentwallet.wallet_service.dto.CounterpartyResponse;
import com.paymentwallet.wallet_service.dto.TransactionResponse;
import com.paymentwallet.wallet_service.dto.UserLookupResponse;
import com.paymentwallet.wallet_service.entity.Transfer;
import com.paymentwallet.wallet_service.entity.Wallet;
import com.paymentwallet.wallet_service.exception.WalletNotFoundException;
import com.paymentwallet.wallet_service.repository.TransferRepository;
import com.paymentwallet.wallet_service.repository.WalletRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.paymentwallet.wallet_service.dto.PagedTransactionResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final UserServiceClient userServiceClient;

    public TransactionService(
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            UserServiceClient userServiceClient
    ) {
        this.walletRepository =
                walletRepository;

        this.transferRepository =
                transferRepository;

        this.userServiceClient =
                userServiceClient;
    }

    public PagedTransactionResponse getTransactions(
            Long userId,
            String authorizationHeader,
            int page,
            int size
    ) {

        /*
         * Defensive pagination validation.
         */
        int safePage =
                Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        100
                );

        Wallet currentWallet =
                walletRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new WalletNotFoundException(
                                                "Wallet not found"
                                        )
                        );

        /*
         * Pagination happens in the database.
         *
         * Equivalent conceptually to:
         *
         * ORDER BY created_at DESC
         * LIMIT ?
         * OFFSET ?
         */
        PageRequest pageRequest =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<Transfer> transferPage =
                transferRepository
                        .findBySenderWalletIdOrReceiverWalletId(
                                currentWallet.getId(),
                                currentWallet.getId(),
                                pageRequest
                        );

        List<Transfer> transfers =
                transferPage.getContent();

        /*
         * Empty page.
         *
         * No reason to call User Service.
         */
        if (transfers.isEmpty()) {

            return new PagedTransactionResponse(
                    List.of(),
                    transferPage.getNumber(),
                    transferPage.getSize(),
                    transferPage.getTotalElements(),
                    transferPage.getTotalPages(),
                    transferPage.isFirst(),
                    transferPage.isLast()
            );
        }

        /*
         * Determine the other wallet involved in
         * every transaction on THIS page.
         */
        Set<Long> counterpartyWalletIds =
                transfers
                        .stream()
                        .map(
                                transfer ->
                                        getCounterpartyWalletId(
                                                transfer,
                                                currentWallet.getId()
                                        )
                        )
                        .collect(
                                Collectors.toSet()
                        );

        /*
         * Fetch counterpart wallets in one DB query.
         */
        List<Wallet> counterpartyWallets =
                walletRepository
                        .findAllById(
                                counterpartyWalletIds
                        );

        Map<Long, Wallet> walletsById =
                counterpartyWallets
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Wallet::getId,
                                        Function.identity()
                                )
                        );

        /*
         * Extract unique User Service IDs.
         */
        List<Long> counterpartyUserIds =
                counterpartyWallets
                        .stream()
                        .map(Wallet::getUserId)
                        .distinct()
                        .toList();

        /*
         * ONE remote call for all users on this page.
         */
        List<UserLookupResponse> users =
                userServiceClient
                        .findUsersByIds(
                                counterpartyUserIds,
                                authorizationHeader
                        );

        Map<Long, UserLookupResponse> usersById =
                users
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        UserLookupResponse::userId,
                                        Function.identity()
                                )
                        );

        List<TransactionResponse> content =
                transfers
                        .stream()
                        .map(
                                transfer ->
                                        toResponse(
                                                transfer,
                                                currentWallet.getId(),
                                                walletsById,
                                                usersById
                                        )
                        )
                        .toList();

        return new PagedTransactionResponse(
                content,
                transferPage.getNumber(),
                transferPage.getSize(),
                transferPage.getTotalElements(),
                transferPage.getTotalPages(),
                transferPage.isFirst(),
                transferPage.isLast()
        );
    }

    public TransactionResponse getTransaction(
            Long userId,
            Long transferId,
            String authorizationHeader
    ) {

        Wallet currentWallet =
                walletRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new WalletNotFoundException(
                                                "Wallet not found"
                                        )
                        );

        Transfer transfer =
                transferRepository
                        .findById(transferId)
                        .orElseThrow(
                                () ->
                                        new WalletNotFoundException(
                                                "Transaction not found"
                                        )
                        );

        boolean belongsToUser =
                transfer
                        .getSenderWalletId()
                        .equals(
                                currentWallet.getId()
                        )
                        ||
                        transfer
                                .getReceiverWalletId()
                                .equals(
                                        currentWallet.getId()
                                );

        /*
         * Important security check.
         *
         * Knowing a transfer ID is not enough to access
         * another user's transaction.
         */
        if (!belongsToUser) {
            throw new WalletNotFoundException(
                    "Transaction not found"
            );
        }

        Long counterpartyWalletId =
                getCounterpartyWalletId(
                        transfer,
                        currentWallet.getId()
                );

        Wallet counterpartyWallet =
                walletRepository
                        .findById(
                                counterpartyWalletId
                        )
                        .orElseThrow(
                                () ->
                                        new WalletNotFoundException(
                                                "Counterparty wallet not found"
                                        )
                        );

        List<UserLookupResponse> users =
                userServiceClient.findUsersByIds(
                        List.of(
                                counterpartyWallet.getUserId()
                        ),
                        authorizationHeader
                );

        UserLookupResponse counterpartyUser =
                users
                        .stream()
                        .findFirst()
                        .orElse(null);

        return toResponse(
                transfer,
                currentWallet.getId(),
                counterpartyUser
        );
    }

    private Long getCounterpartyWalletId(
            Transfer transfer,
            Long currentWalletId
    ) {

        if (transfer
                .getSenderWalletId()
                .equals(currentWalletId)) {

            return transfer
                    .getReceiverWalletId();
        }

        return transfer
                .getSenderWalletId();
    }

    private TransactionResponse toResponse(
            Transfer transfer,
            Long currentWalletId,
            Map<Long, Wallet> walletsById,
            Map<Long, UserLookupResponse> usersById
    ) {

        Long counterpartyWalletId =
                getCounterpartyWalletId(
                        transfer,
                        currentWalletId
                );

        Wallet counterpartyWallet =
                walletsById.get(
                        counterpartyWalletId
                );

        UserLookupResponse counterpartyUser =
                null;

        if (counterpartyWallet != null) {
            counterpartyUser =
                    usersById.get(
                            counterpartyWallet.getUserId()
                    );
        }

        return toResponse(
                transfer,
                currentWalletId,
                counterpartyUser
        );
    }

    private TransactionResponse toResponse(
            Transfer transfer,
            Long currentWalletId,
            UserLookupResponse counterpartyUser
    ) {

        String type =
                transfer
                        .getSenderWalletId()
                        .equals(currentWalletId)
                        ? "SENT"
                        : "RECEIVED";

        CounterpartyResponse counterparty =
                counterpartyUser == null
                        ? null
                        : new CounterpartyResponse(
                        counterpartyUser.name(),
                        counterpartyUser.walletPayId()
                );

        return new TransactionResponse(
                transfer.getId(),
                transfer.getAmount(),
                type,
                transfer.getStatus().name(),
                transfer.getCreatedAt(),
                counterparty
        );
    }
}