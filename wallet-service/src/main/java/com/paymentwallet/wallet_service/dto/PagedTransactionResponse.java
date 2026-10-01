package com.paymentwallet.wallet_service.dto;

import java.util.List;

public record PagedTransactionResponse(

        List<TransactionResponse> content,

        int page,

        int size,

        long totalElements,

        int totalPages,

        boolean first,

        boolean last

) {
}