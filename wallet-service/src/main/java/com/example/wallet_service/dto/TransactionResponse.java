package com.example.wallet_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionResponse {

    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String type;
    private BigDecimal balance;

    public TransactionResponse(
            UUID transactionId,
            UUID userId,
            BigDecimal amount,
            String type,
            BigDecimal balance
    ) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.balance = balance;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}