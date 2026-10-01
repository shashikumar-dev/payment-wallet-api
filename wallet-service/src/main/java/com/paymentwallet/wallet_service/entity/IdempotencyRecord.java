package com.paymentwallet.wallet_service.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_idempotency_key",
                columnNames = {
                        "user_id",
                        "idempotency_key"
                }
        )
)
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private Long userId;

    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String idempotencyKey;

    @Column(
            name = "transfer_id",
            nullable = false,
            updatable = false
    )
    private Long transferId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(
            Long userId,
            String idempotencyKey,
            Long transferId
    ) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.transferId = transferId;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getTransferId() {
        return transferId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}