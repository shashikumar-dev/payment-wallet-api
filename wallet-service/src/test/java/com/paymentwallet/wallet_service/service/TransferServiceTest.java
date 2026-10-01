package com.paymentwallet.wallet_service.service;

import com.paymentwallet.wallet_service.client.UserServiceClient;
import com.paymentwallet.wallet_service.dto.TransferRequest;
import com.paymentwallet.wallet_service.dto.TransferResponse;
import com.paymentwallet.wallet_service.dto.UserLookupResponse;
import com.paymentwallet.wallet_service.exception.InvalidTransferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private TransferTransactionService transferTransactionService;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(
                userServiceClient,
                transferTransactionService
        );
    }

    @Test
    void shouldResolveWalletPayIdAndTransferMoney() {

        Long senderUserId = 1L;
        String idempotencyKey = "test-key-001";
        String authorizationHeader = "Bearer test-token";

        TransferRequest request =
                new TransferRequest(
                        "priya@walletpay",
                        new BigDecimal("100.00")
                );

        UserLookupResponse receiver =
                new UserLookupResponse(
                        2L,
                        "Priya",
                        "priya@walletpay"
                );

        TransferResponse expectedResponse =
                new TransferResponse(
                        10L,
                        100L,
                        200L,
                        new BigDecimal("100.00"),
                        "SUCCESS",
                        Instant.now()
                );

        when(userServiceClient.findByWalletPayId(
                "priya@walletpay",
                authorizationHeader
        )).thenReturn(receiver);

        when(transferTransactionService.execute(
                senderUserId,
                2L,
                new BigDecimal("100.00"),
                idempotencyKey
        )).thenReturn(expectedResponse);

        TransferResponse actualResponse =
                transferService.transfer(
                        senderUserId,
                        idempotencyKey,
                        authorizationHeader,
                        request
                );

        assertNotNull(actualResponse);
        assertEquals(
                expectedResponse.transferId(),
                actualResponse.transferId()
        );

        verify(userServiceClient).findByWalletPayId(
                "priya@walletpay",
                authorizationHeader
        );

        verify(transferTransactionService).execute(
                senderUserId,
                2L,
                new BigDecimal("100.00"),
                idempotencyKey
        );
    }

    @Test
    void shouldRejectSelfTransfer() {

        Long senderUserId = 1L;

        TransferRequest request =
                new TransferRequest(
                        "shashi@walletpay",
                        new BigDecimal("100.00")
                );

        when(userServiceClient.findByWalletPayId(
                "shashi@walletpay",
                "Bearer token"
        )).thenReturn(
                new UserLookupResponse(
                        1L,
                        "Shashi",
                        "shashi@walletpay"
                )
        );

        assertThrows(
                InvalidTransferException.class,
                () -> transferService.transfer(
                        senderUserId,
                        "key-001",
                        "Bearer token",
                        request
                )
        );

        verifyNoInteractions(
                transferTransactionService
        );
    }

    @Test
    void shouldRejectMissingIdempotencyKey() {

        TransferRequest request =
                new TransferRequest(
                        "priya@walletpay",
                        new BigDecimal("100.00")
                );

        assertThrows(
                InvalidTransferException.class,
                () -> transferService.transfer(
                        1L,
                        "",
                        "Bearer token",
                        request
                )
        );

        verifyNoInteractions(userServiceClient);
        verifyNoInteractions(
                transferTransactionService
        );
    }
}