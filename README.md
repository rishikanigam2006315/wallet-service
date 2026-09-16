# Wallet Service

A production-oriented wallet transaction service built with Java and Spring Boot, focused on reliable transaction processing, idempotency, and safe concurrent balance updates.

The project demonstrates how to handle race conditions and duplicate transaction requests using database-level locking and transactional consistency.

---

## Overview

Wallet Service provides a REST API for processing debit transactions against user wallets.

The implementation focuses on two important backend concerns:

- Preventing duplicate transaction processing through idempotency
- Preventing race conditions during concurrent wallet balance updates

The application uses an H2 in-memory database and includes integration tests covering concurrent transaction scenarios.

---

## Tech Stack

| Technology | Usage |
|------------|-------|
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
