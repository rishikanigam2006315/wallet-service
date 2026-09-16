package com.example.wallet_service;

import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.repository.TransactionRepository;
import com.example.wallet_service.repository.WalletRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {

        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        userId = UUID.randomUUID();

        Wallet wallet = new Wallet(
                userId,
                new BigDecimal("500.00")
        );

        walletRepository.save(wallet);
    }

    @Test
    @DisplayName("Processes a single valid debit transaction successfully.")
    public void processesSingleValidDebitTransaction() throws Exception {

        UUID transactionId = UUID.randomUUID();

        String request = """
                {
                    "transactionId": "%s",
                    "userId": "%s",
                    "amount": 100.00,
                    "type": "DEBIT"
                }
                """.formatted(transactionId, userId);

        mockMvc.perform(
                        post("/api/v1/transactions/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk());

        Wallet wallet = walletRepository
                .findByUserId(userId)
                .orElseThrow();

        assertEquals(
                new BigDecimal("400.00"),
                wallet.getBalance()
        );

        System.out.println();
        System.out.println("==============================================");
        System.out.println("TEST: Processes a single valid debit transaction successfully.");
        System.out.println("RESULT: PASSED");
        System.out.println("Initial Balance: ₹500.00");
        System.out.println("Debit Amount: ₹100.00");
        System.out.println("Final Balance: ₹" + wallet.getBalance());
        System.out.println("==============================================");
    }


    @Test
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    public void processesDuplicateTransactionsConcurrently()
            throws Exception {

        UUID transactionId = UUID.randomUUID();

        String request = """
                {
                    "transactionId": "%s",
                    "userId": "%s",
                    "amount": 100.00,
                    "type": "DEBIT"
                }
                """.formatted(transactionId, userId);

        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 3; i++) {

            futures.add(
                    executor.submit(() -> {

                        startLatch.await();

                        return mockMvc.perform(
                                        post("/api/v1/transactions/process")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    })
            );
        }

        startLatch.countDown();

        List<Integer> statuses = new ArrayList<>();

        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }

        executor.shutdown();

        Wallet wallet = walletRepository
                .findByUserId(userId)
                .orElseThrow();

        long successfulRequests =
                statuses.stream()
                        .filter(status -> status == 200)
                        .count();

        assertTrue(successfulRequests >= 1);

        assertEquals(
                new BigDecimal("400.00"),
                wallet.getBalance()
        );

        assertEquals(
                1,
                transactionRepository
                        .findByTransactionId(transactionId)
                        .stream()
                        .count()
        );

        System.out.println();
        System.out.println("==============================================");
        System.out.println("TEST: Sends 3 identical transactionIDs simultaneously.");
        System.out.println("RESULT: PASSED");
        System.out.println("Requests Sent: 3");
        System.out.println("Successful/Original Transaction: 1");
        System.out.println("Final Balance: ₹" + wallet.getBalance());
        System.out.println("==============================================");
    }


    @Test
    @DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
    public void handlesConcurrentDebitRaceCondition()
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            UUID transactionId = UUID.randomUUID();

            String request = """
                    {
                        "transactionId": "%s",
                        "userId": "%s",
                        "amount": 100.00,
                        "type": "DEBIT"
                    }
                    """.formatted(transactionId, userId);

            futures.add(
                    executor.submit(() -> {

                        startLatch.await();

                        return mockMvc.perform(
                                        post("/api/v1/transactions/process")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    })
            );
        }

        startLatch.countDown();

        List<Integer> statuses = new ArrayList<>();

        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }

        executor.shutdown();

        long successfulRequests =
                statuses.stream()
                        .filter(status -> status == 200)
                        .count();

        long failedRequests =
                statuses.stream()
                        .filter(status -> status == 409)
                        .count();

        Wallet wallet = walletRepository
                .findByUserId(userId)
                .orElseThrow();

        assertEquals(5, successfulRequests);

        assertEquals(5, failedRequests);

        assertEquals(
                new BigDecimal("0.00"),
                wallet.getBalance()
        );

        System.out.println();
        System.out.println("==============================================");
        System.out.println("TEST: Sends 10 concurrent debit requests of ₹100.");
        System.out.println("RESULT: PASSED");
        System.out.println("Requests Sent: 10");
        System.out.println("Successful Requests: " + successfulRequests);
        System.out.println("Failed Requests: " + failedRequests);
        System.out.println("Final Balance: ₹" + wallet.getBalance());
        System.out.println("==============================================");
    }
}
