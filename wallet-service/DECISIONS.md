# Design Decisions

## 1. How did you prevent the concurrency race condition?

The wallet balance update is protected using database-level pessimistic locking.

For every transaction, the wallet row is locked using `findByUserIdForUpdate()` before checking the transaction and modifying the balance.

The service method is also marked with `@Transactional`, so the lock remains active for the duration of the transaction.

This ensures that when multiple debit requests for the same wallet arrive concurrently, only one request can modify the wallet at a time. Other requests wait for the lock to be released and then read the latest balance.

Because the balance check and balance update happen while the wallet row is locked, two concurrent requests cannot both deduct money based on the same old balance.

This prevents the wallet balance from becoming negative.

---

## 2. How did you handle idempotency for duplicate transaction IDs?

The `transactionId` has a UNIQUE constraint in the database.

Before processing a transaction, the service checks whether the transaction ID already exists.

The wallet row is locked before this check. This is important because simply checking the transaction ID first would create a race condition where multiple concurrent requests could all see that the transaction does not exist.

With the wallet lock, concurrent requests for the same wallet are processed sequentially.

The first request creates the transaction and deducts the amount. Subsequent requests find the existing transaction and return its result without deducting the wallet balance again.

The database UNIQUE constraint provides an additional safety guarantee that the same transaction ID cannot be inserted more than once.

---

## 3. Where was the AI-generated suggestion wrong or suboptimal?

An initial implementation checked whether the transaction already existed before acquiring the wallet lock.

The flow was:

1. Check transaction ID.
2. If it does not exist, lock the wallet.
3. Deduct the balance.
4. Insert the transaction.

This was suboptimal for concurrent identical requests because multiple requests could perform the initial transaction lookup at the same time and all see that the transaction did not exist.

They could then continue processing concurrently and attempt to insert the same transaction ID.

The database correctly rejected the duplicate inserts through the UNIQUE constraint, but this resulted in `DataIntegrityViolationException` instead of clean idempotent handling.

The implementation was therefore changed to acquire the wallet lock first and perform the transaction lookup after acquiring the lock.

This makes the idempotency check and wallet update part of the same serialized critical section.

---

## 4. Why was database-level locking chosen?

Database-level locking was chosen instead of an in-memory Java lock because the wallet balance is persisted in the database and the concurrency guarantee should be enforced at the database level.

Using a database lock also makes the solution safer if multiple application instances are running, because the lock is managed by the database rather than by a single JVM.

The solution uses pessimistic row-level locking through JPA.

---

## 5. Transactional behavior

The transaction processing method uses `@Transactional`.

The wallet lock, balance validation, balance update, and transaction insertion occur within the same database transaction.

If the transaction fails, the database changes are rolled back, preventing a partial balance update.

---

## 6. Testing

The implementation was verified using integration tests with Spring Boot, MockMvc, and an H2 in-memory database.

The following scenarios pass:

- A single valid debit decreases the wallet balance correctly.
- Three concurrent requests with the same transaction ID result in only one balance deduction.
- Ten concurrent ₹100 debit requests against a ₹500 wallet result in exactly five successful requests, five insufficient-funds failures, and a final balance of ₹0.