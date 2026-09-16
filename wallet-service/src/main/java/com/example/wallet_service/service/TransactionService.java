package com.example.wallet_service.service;

import com.example.wallet_service.dto.TransactionRequest;
import com.example.wallet_service.dto.TransactionResponse;
import com.example.wallet_service.entity.Transaction;
import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.exception.InsufficientFundsException;
import com.example.wallet_service.repository.TransactionRepository;
import com.example.wallet_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse process(TransactionRequest request) {
        Wallet wallet = walletRepository
                .findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Wallet not found")
                );
        var existingTransaction =
                transactionRepository.findByTransactionId(
                        request.getTransactionId()
                );

        if (existingTransaction.isPresent()) {

            Transaction transaction = existingTransaction.get();

            return new TransactionResponse(
                    transaction.getTransactionId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getType(),
                    wallet.getBalance()
            );
        }
        if ("DEBIT".equalsIgnoreCase(request.getType())) {

            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds"
                );
            }

            wallet.setBalance(
                    wallet.getBalance()
                            .subtract(request.getAmount())
            );
        }
        Transaction transaction = new Transaction(
                request.getTransactionId(),
                request.getUserId(),
                request.getAmount(),
                request.getType()
        );

        transactionRepository.save(transaction);
        walletRepository.save(wallet);

        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getType(),
                wallet.getBalance()
        );
    }
}