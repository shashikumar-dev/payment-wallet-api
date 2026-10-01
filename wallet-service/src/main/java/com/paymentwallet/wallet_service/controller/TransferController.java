package com.paymentwallet.wallet_service.controller;

import com.paymentwallet.wallet_service.dto.TransferRequest;
import com.paymentwallet.wallet_service.dto.TransferResponse;
import com.paymentwallet.wallet_service.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(
            TransferService transferService
    ) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            Authentication authentication,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @RequestHeader("Authorization")
            String authorizationHeader,

            @Valid
            @RequestBody
            TransferRequest request
    ) {

        Long senderUserId =
                (Long) authentication.getPrincipal();

        TransferResponse response =
                transferService.transfer(
                        senderUserId,
                        idempotencyKey,
                        authorizationHeader,
                        request
                );

        return ResponseEntity.ok(response);
    }
}