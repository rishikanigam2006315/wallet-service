# Wallet Service

A production-oriented wallet transaction service built with **Java and Spring Boot**, focused on reliable transaction processing, idempotency, and safe concurrent balance updates.

The project demonstrates how to handle race conditions and duplicate transaction requests using database-level locking and transactional consistency.

---

## Overview

Wallet Service provides a REST API for processing debit transactions against user wallets.

The implementation focuses on two important backend concerns:

- Preventing duplicate transaction processing through idempotency
- Preventing race conditions during concurrent wallet balance updates

The application uses an **H2 in-memory database** and includes integration tests covering concurrent transaction scenarios.

---

## Tech Stack

| Technology | Usage |
|---|---|
| Java 17+ | Backend development |
| Spring Boot 4.1.1 | Application framework |
| Spring Web MVC | REST API |
| Spring Data JPA | Data persistence |
| Hibernate | ORM |
| H2 Database | In-memory database |
| JUnit 5 | Testing |
| MockMvc | API integration testing |
| Maven | Build & dependency management |

---

## Core Features

### Transaction Processing

Processes wallet transactions through a REST endpoint:

```http
POST /api/v1/transactions/process
```

The API accepts transaction details including:

- Transaction ID
- User ID
- Transaction amount
- Transaction type

---

### Idempotency

Each transaction is identified using a unique `transactionId`.

If the same transaction request is received multiple times, the transaction is processed only once and the wallet balance is not deducted again.

A database-level `UNIQUE` constraint on `transactionId` provides an additional safeguard against duplicate transactions.

---

### Concurrency Control

The wallet row is locked using **database-level pessimistic locking** before modifying the balance.

This prevents multiple concurrent debit requests from reading the same wallet balance and incorrectly deducting money simultaneously.

---

### Insufficient Funds Protection

Before processing a debit, the service checks whether the wallet has sufficient balance.

If the available balance is lower than the requested amount, the transaction fails with an insufficient funds error.

This prevents the wallet balance from becoming negative.

---

### Transactional Consistency

The transaction processing operation uses `@Transactional`.

Wallet locking, balance validation, balance update, and transaction creation are performed within the same database transaction.

If the transaction fails, the database changes are rolled back.

---

## API

### Process Transaction

**Endpoint**

```http
POST /api/v1/transactions/process
```

**Content-Type**

```http
application/json
```

### Request

```json
{
  "transactionId": "UUID",
  "userId": "UUID",
  "amount": 250.00,
  "type": "DEBIT"
}
```

### Example Response

```json
{
  "transactionId": "UUID",
  "userId": "UUID",
  "amount": 250.00,
  "type": "DEBIT",
  "balance": 250.00
}
```

---

## Concurrency Strategy

The transaction processing flow is:

```text
Request
   ↓
Lock Wallet Row
   ↓
Check Transaction ID
   ↓
Already Exists?
   ├── Yes → Return Existing Transaction
   │
   └── No
        ↓
   Validate Balance
        ↓
   Deduct Amount
        ↓
   Save Transaction
        ↓
   Commit Transaction
```

The wallet row is locked before checking the transaction ID and modifying the balance.

This ensures that concurrent requests for the same wallet are processed sequentially and always work with the latest wallet balance.

---

## Integration Testing

The project includes integration tests using:

- Spring Boot Test
- MockMvc
- JUnit 5
- H2 in-memory database

### Test Scenarios

| Test Scenario | Result |
|---|---|
| Single valid ₹100 debit from ₹500 | ✅ PASS |
| 3 concurrent requests with same transaction ID | ✅ PASS |
| 10 concurrent ₹100 debit requests from ₹500 | ✅ PASS |
| Duplicate balance deduction prevention | ✅ PASS |
| Negative balance prevention | ✅ PASS |

### Concurrent Debit Test

For a wallet with an initial balance of **₹500**, 10 concurrent requests of **₹100** were processed.

```text
Total Requests       : 10
Successful Requests  : 5
Failed Requests      : 5
Failure Reason       : Insufficient Funds
Final Balance        : ₹0
```

### Duplicate Transaction Test

Three concurrent requests were sent using the same transaction ID.

```text
Requests Sent         : 3
Transaction Processed : 1
Balance Deduction     : 1
Final Balance         : ₹400
```

This verifies that duplicate requests do not cause multiple balance deductions.

---

## Project Structure

```text
wallet-service/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/wallet_service/
│   │           ├── controller/
│   │           ├── service/
│   │           ├── repository/
│   │           ├── entity/
│   │           ├── dto/
│   │           └── exception/
│   │
│   └── test/
│       └── java/
│           └── com/example/wallet_service/
│               └── TransactionIntegrationTest.java
│
├── DECISIONS.md
├── pom.xml
└── README.md
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven

No external database setup is required because the project uses an H2 in-memory database.

### Clone Repository

```bash
git clone <YOUR_REPOSITORY_URL>
```

```bash
cd wallet-service
```

### Run Application

```bash
mvn spring-boot:run
```

### Run Tests

```bash
mvn test
```

---

## Database

The application uses an **H2 in-memory database**.

The database is created automatically when the application starts, so no external database installation or configuration is required.

---

## Design Decisions

Detailed design and concurrency decisions are documented in:

**[DECISIONS.md](DECISIONS.md)**

The document explains:

- How the concurrency race condition was prevented
- Why pessimistic database locking was used
- How transaction idempotency was implemented
- Why checking the transaction ID before locking the wallet was suboptimal
- How transactional consistency is maintained
- Integration test results

---

## Key Implementation Highlights

### Database-Level Locking

The wallet repository uses a pessimistic write lock to ensure that only one concurrent transaction can modify a wallet balance at a time.

### Idempotency

The service checks for an existing transaction after acquiring the wallet lock.

This ensures that concurrent duplicate requests are handled safely.

### Atomic Transaction Processing

The complete operation runs inside a database transaction using:

```java
@Transactional
```

This keeps balance updates and transaction creation consistent.

---

## Author

**Rishika Nigam**

Java Backend Developer | Spring Boot | JPA 
