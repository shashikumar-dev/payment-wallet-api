package com.paymentwallet.wallet_service.repository;

import com.paymentwallet.wallet_service.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository
        extends JpaRepository<Transfer, Long> {

    Page<Transfer>
    findBySenderWalletIdOrReceiverWalletId(
            Long senderWalletId,
            Long receiverWalletId,
            Pageable pageable
    );
}