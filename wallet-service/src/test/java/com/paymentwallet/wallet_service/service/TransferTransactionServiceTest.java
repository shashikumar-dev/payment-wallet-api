package com.paymentwallet.wallet_service.service;

import com.paymentwallet.wallet_service.dto.TransferResponse;
import com.paymentwallet.wallet_service.entity.IdempotencyRecord;
import com.paymentwallet.wallet_service.entity.Transfer;
import com.paymentwallet.wallet_service.entity.Wallet;
import com.paymentwallet.wallet_service.exception.InsufficientBalanceException;
import com.paymentwallet.wallet_service.repository.IdempotencyRecordRepository;
import com.paymentwallet.wallet_service.repository.TransferRepository;
import com.paymentwallet.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferTransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private Wallet senderWallet;

    @Mock
    private Wallet receiverWallet;

    private TransferTransactionService transferTransactionService;

    @BeforeEach
    void setUp() {

        transferTransactionService =
                new TransferTransactionService(
                        walletRepository,
                        transferRepository,
                        idempotencyRecordRepository
                );
    }

    @Test
    void shouldTransferMoneySuccessfully() {

        Long senderUserId = 1L;
        Long receiverUserId = 2L;

        Long senderWalletId = 10L;
        Long receiverWalletId = 20L;

        BigDecimal amount =
                new BigDecimal("100.00");

        String idempotencyKey =
                "transfer-key-001";

        /*
         * No existing request with this idempotency key.
         */
        when(
                idempotencyRecordRepository
                        .findByUserIdAndIdempotencyKey(
                                senderUserId,
                                idempotencyKey
                        )
        ).thenReturn(Optional.empty());

        /*
         * First wallet lookup.
         */
        when(walletRepository.findByUserId(senderUserId))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserId(receiverUserId))
                .thenReturn(Optional.of(receiverWallet));

        /*
         * Wallet IDs.
         *
         * These are important because the service uses
         * wallet IDs to determine lock ordering.
         */
        when(senderWallet.getId())
                .thenReturn(senderWalletId);

        when(receiverWallet.getId())
                .thenReturn(receiverWalletId);

        /*
         * Database locking.
         *
         * Because:
         *
         * 10 < 20
         *
         * wallet 10 gets locked first.
         */
        when(
                walletRepository
                        .findByIdForUpdate(senderWalletId)
        ).thenReturn(Optional.of(senderWallet));

        when(
                walletRepository
                        .findByIdForUpdate(receiverWalletId)
        ).thenReturn(Optional.of(receiverWallet));

        /*
         * Sender has enough money.
         */
        when(senderWallet.getBalance())
                .thenReturn(
                        new BigDecimal("500.00")
                );

        /*
         * Create the Transfer that will represent what
         * the repository returns after save().
         */
        Transfer savedTransfer =
                new Transfer(
                        senderWalletId,
                        receiverWalletId,
                        amount
                );

        when(transferRepository.save(any(Transfer.class)))
                .thenReturn(savedTransfer);

        /*
         * Execute.
         */
        TransferResponse response =
                transferTransactionService.execute(
                        senderUserId,
                        receiverUserId,
                        amount,
                        idempotencyKey
                );

        /*
         * Verify debit and credit.
         */
        verify(senderWallet)
                .debit(amount);

        verify(receiverWallet)
                .credit(amount);

        /*
         * Verify transfer was persisted.
         */
        ArgumentCaptor<Transfer> transferCaptor =
                ArgumentCaptor.forClass(
                        Transfer.class
                );

        verify(transferRepository)
                .save(transferCaptor.capture());

        Transfer capturedTransfer =
                transferCaptor.getValue();

        assertEquals(
                senderWalletId,
                capturedTransfer.getSenderWalletId()
        );

        assertEquals(
                receiverWalletId,
                capturedTransfer.getReceiverWalletId()
        );

        assertEquals(
                amount,
                capturedTransfer.getAmount()
        );

        /*
         * Verify idempotency record was created.
         */
        verify(idempotencyRecordRepository)
                .save(any(IdempotencyRecord.class));

        /*
         * Verify response.
         */
        assertNotNull(response);

        assertEquals(
                amount,
                response.amount()
        );
    }

    @Test
    void shouldRejectTransferWhenBalanceIsInsufficient() {

        Long senderUserId = 1L;
        Long receiverUserId = 2L;

        Long senderWalletId = 10L;
        Long receiverWalletId = 20L;

        BigDecimal transferAmount =
                new BigDecimal("100.00");

        String idempotencyKey =
                "transfer-key-002";

        when(
                idempotencyRecordRepository
                        .findByUserIdAndIdempotencyKey(
                                senderUserId,
                                idempotencyKey
                        )
        ).thenReturn(Optional.empty());

        when(walletRepository.findByUserId(senderUserId))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserId(receiverUserId))
                .thenReturn(Optional.of(receiverWallet));

        when(senderWallet.getId())
                .thenReturn(senderWalletId);

        when(receiverWallet.getId())
                .thenReturn(receiverWalletId);

        when(
                walletRepository
                        .findByIdForUpdate(senderWalletId)
        ).thenReturn(Optional.of(senderWallet));

        when(
                walletRepository
                        .findByIdForUpdate(receiverWalletId)
        ).thenReturn(Optional.of(receiverWallet));

        /*
         * Sender has only ₹50 but wants to send ₹100.
         */
        when(senderWallet.getBalance())
                .thenReturn(
                        new BigDecimal("50.00")
                );

        assertThrows(
                InsufficientBalanceException.class,
                () ->
                        transferTransactionService.execute(
                                senderUserId,
                                receiverUserId,
                                transferAmount,
                                idempotencyKey
                        )
        );

        /*
         * Money must NOT move.
         */
        verify(senderWallet, never())
                .debit(any());

        verify(receiverWallet, never())
                .credit(any());

        /*
         * No transfer should be stored.
         */
        verify(transferRepository, never())
                .save(any());

        /*
         * No idempotency record should be stored.
         */
        verify(idempotencyRecordRepository, never())
                .save(any());
    }
}