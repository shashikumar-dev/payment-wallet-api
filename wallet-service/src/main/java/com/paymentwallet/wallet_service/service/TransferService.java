package com.paymentwallet.wallet_service.service;

import com.paymentwallet.wallet_service.client.UserServiceClient;
import com.paymentwallet.wallet_service.dto.TransferRequest;
import com.paymentwallet.wallet_service.dto.TransferResponse;
import com.paymentwallet.wallet_service.dto.UserLookupResponse;
import com.paymentwallet.wallet_service.exception.InvalidTransferException;
import org.springframework.stereotype.Service;

@Service
public class TransferService {

    private final UserServiceClient userServiceClient;
    private final TransferTransactionService transferTransactionService;

    public TransferService(
            UserServiceClient userServiceClient,
            TransferTransactionService transferTransactionService
    ) {
        this.userServiceClient = userServiceClient;
        this.transferTransactionService = transferTransactionService;
    }

    public TransferResponse transfer(
            Long senderUserId,
            String idempotencyKey,
            String authorizationHeader,
            TransferRequest request
    ) {

        validateIdempotencyKey(idempotencyKey);

        /*
         * Resolve the public WalletPay ID BEFORE starting
         * the database transaction.
         *
         * Example:
         *
         * priya@walletpay
         *       ↓
         * User Service
         *       ↓
         * userId = 2
         *
         * We deliberately don't hold wallet database locks
         * while waiting for an HTTP response.
         */
        UserLookupResponse receiver =
                userServiceClient.findByWalletPayId(
                        request.receiverWalletPayId(),
                        authorizationHeader
                );

        /*
         * User Service should always return a user ID,
         * but keep this defensive check at the boundary.
         */
        if (receiver == null || receiver.userId() == null) {
            throw new InvalidTransferException(
                    "Unable to resolve receiver WalletPay ID"
            );
        }

        /*
         * The sender entered their own WalletPay ID.
         *
         * We can determine this after resolving the public
         * WalletPay ID to its internal user ID.
         */
        if (senderUserId.equals(receiver.userId())) {
            throw new InvalidTransferException(
                    "Cannot transfer money to your own wallet"
            );
        }

        /*
         * Now enter the transactional part.
         *
         * No remote HTTP call occurs inside this method.
         */
        return transferTransactionService.execute(
                senderUserId,
                receiver.userId(),
                request.amount(),
                idempotencyKey
        );
    }

    private void validateIdempotencyKey(
            String idempotencyKey
    ) {

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new InvalidTransferException(
                    "Idempotency-Key is required"
            );
        }

        if (idempotencyKey.length() > 100) {
            throw new InvalidTransferException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }
    }
}