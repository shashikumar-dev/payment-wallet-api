package com.paymentwallet.wallet_service.service;

import com.paymentwallet.wallet_service.dto.TransferResponse;
import com.paymentwallet.wallet_service.entity.IdempotencyRecord;
import com.paymentwallet.wallet_service.entity.Transfer;
import com.paymentwallet.wallet_service.entity.Wallet;
import com.paymentwallet.wallet_service.exception.InsufficientBalanceException;
import com.paymentwallet.wallet_service.exception.InvalidTransferException;
import com.paymentwallet.wallet_service.exception.WalletNotFoundException;
import com.paymentwallet.wallet_service.repository.IdempotencyRecordRepository;
import com.paymentwallet.wallet_service.repository.TransferRepository;
import com.paymentwallet.wallet_service.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransferTransactionService {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public TransferTransactionService(
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.idempotencyRecordRepository =
                idempotencyRecordRepository;
    }

    @Transactional
    public TransferResponse execute(
            Long senderUserId,
            Long receiverUserId,
            BigDecimal amount,
            String idempotencyKey
    ) {

        /*
         * Defensive validation.
         *
         * TransferService already prevents this, but keeping
         * this check here protects the transactional method
         * if it is called from somewhere else in the future.
         */
        if (senderUserId.equals(receiverUserId)) {
            throw new InvalidTransferException(
                    "Cannot transfer money to your own wallet"
            );
        }

        /*
         * STEP 1
         *
         * Check whether this logical request has already
         * completed successfully.
         */
        Optional<IdempotencyRecord> existingRecord =
                idempotencyRecordRepository
                        .findByUserIdAndIdempotencyKey(
                                senderUserId,
                                idempotencyKey
                        );

        if (existingRecord.isPresent()) {

            Long existingTransferId =
                    existingRecord
                            .get()
                            .getTransferId();

            Transfer existingTransfer =
                    transferRepository
                            .findById(existingTransferId)
                            .orElseThrow(
                                    () -> new IllegalStateException(
                                            "Transfer record not found"
                                    )
                            );

            return toResponse(existingTransfer);
        }

        /*
         * STEP 2
         *
         * Find the sender wallet.
         *
         * This lookup is NOT the locked version yet.
         * We need its wallet ID so that we can establish
         * deterministic lock ordering.
         */
        Wallet senderWallet =
                walletRepository
                        .findByUserId(senderUserId)
                        .orElseThrow(
                                () -> new WalletNotFoundException(
                                        "Sender wallet not found"
                                )
                        );

        /*
         * STEP 3
         *
         * Find receiver wallet using the internal user ID
         * that TransferService resolved from WalletPay ID.
         */
        Wallet receiverWallet =
                walletRepository
                        .findByUserId(receiverUserId)
                        .orElseThrow(
                                () -> new WalletNotFoundException(
                                        "Receiver wallet not found"
                                )
                        );

        /*
         * STEP 4
         *
         * Determine deterministic locking order.
         *
         * We do NOT simply lock sender first.
         *
         * Example:
         *
         * Transfer A:
         * wallet 1 -> wallet 2
         *
         * Transfer B:
         * wallet 2 -> wallet 1
         *
         * Both transactions lock:
         *
         * wallet 1
         * then
         * wallet 2
         *
         * This substantially reduces deadlock risk.
         */
        Long firstWalletId =
                Math.min(
                        senderWallet.getId(),
                        receiverWallet.getId()
                );

        Long secondWalletId =
                Math.max(
                        senderWallet.getId(),
                        receiverWallet.getId()
                );

        /*
         * STEP 5
         *
         * Acquire pessimistic write lock on the first wallet.
         */
        Wallet firstLockedWallet =
                walletRepository
                        .findByIdForUpdate(firstWalletId)
                        .orElseThrow(
                                () -> new WalletNotFoundException(
                                        "Wallet not found"
                                )
                        );

        /*
         * STEP 6
         *
         * Acquire pessimistic write lock on second wallet.
         */
        Wallet secondLockedWallet =
                walletRepository
                        .findByIdForUpdate(secondWalletId)
                        .orElseThrow(
                                () -> new WalletNotFoundException(
                                        "Wallet not found"
                                )
                        );

        /*
         * STEP 7
         *
         * Locking order and transfer direction are different
         * concepts.
         *
         * Determine which locked entity represents sender
         * and which represents receiver.
         */
        Wallet lockedSender;
        Wallet lockedReceiver;

        if (firstLockedWallet
                .getId()
                .equals(senderWallet.getId())) {

            lockedSender = firstLockedWallet;
            lockedReceiver = secondLockedWallet;

        } else {

            lockedSender = secondLockedWallet;
            lockedReceiver = firstLockedWallet;
        }

        /*
         * STEP 8
         *
         * Check the balance AFTER acquiring the lock.
         *
         * Checking the pre-lock senderWallet balance would
         * create a race condition because another transaction
         * may have modified the balance in between.
         */
        if (lockedSender
                .getBalance()
                .compareTo(amount) < 0) {

            throw new InsufficientBalanceException();
        }

        /*
         * STEP 9
         *
         * Move the money.
         */
        lockedSender.debit(amount);
        lockedReceiver.credit(amount);

        /*
         * No walletRepository.save(...) is necessary.
         *
         * Both locked wallets are managed JPA entities inside
         * the current transaction.
         *
         * Hibernate dirty checking detects the changed
         * balances and generates UPDATE statements when the
         * transaction is flushed/committed.
         */

        /*
         * STEP 10
         *
         * Record the successful transfer.
         */
        Transfer transfer =
                new Transfer(
                        lockedSender.getId(),
                        lockedReceiver.getId(),
                        amount
                );

        Transfer savedTransfer =
                transferRepository.save(transfer);

        /*
         * STEP 11
         *
         * Associate this successful transfer with the
         * idempotency key.
         *
         * This happens in the SAME database transaction.
         *
         * Therefore transfer + balance changes +
         * idempotency record either commit together or
         * roll back together.
         */
        idempotencyRecordRepository.save(
                new IdempotencyRecord(
                        senderUserId,
                        idempotencyKey,
                        savedTransfer.getId()
                )
        );

        /*
         * STEP 12
         *
         * Transaction commits after this method completes.
         */
        return toResponse(savedTransfer);
    }

    private TransferResponse toResponse(
            Transfer transfer
    ) {

        return new TransferResponse(
                transfer.getId(),
                transfer.getSenderWalletId(),
                transfer.getReceiverWalletId(),
                transfer.getAmount(),
                transfer.getStatus().name(),
                transfer.getCreatedAt()
        );
    }
}