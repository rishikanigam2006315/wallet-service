package com.example.wallet_service.repository;

import com.example.wallet_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<com.example.wallet_service.entity.Transaction> findByTransactionId(UUID transactionId);
}
