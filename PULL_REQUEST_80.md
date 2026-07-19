# Pull Request

## Summary

This Pull Request implements inventory reporting: a total stock value report and full stock movement history tracking (in/out/adjustments), completing the second item of Phase 6 on the roadmap.

---

## Related Issue

Closes #80

---

## Type of Change

- [x] Feature
- [ ] Bug Fix
- [ ] Refactoring
- [ ] Documentation
- [ ] Performance Improvement
- [x] Test
- [ ] CI/CD
- [ ] Build

---

## What Changed

- Added `MovementType` enum (`IN`, `OUT`, `ADJUSTMENT`) and a `StockMovement` entity recording each stock change against a product.
- Added Flyway migration `V4__stock_movements.sql` creating the `stock_movements` table with an index on `product_id`.
- Added `StockMovementRepository` with `findByProductIdOrderByCreatedAtDesc`.
- Added `InsufficientStockException`, mapped to `409 Conflict` in `GlobalExceptionHandler`, thrown when an `OUT` movement would drive quantity negative.
- Added `StockMovementService`: records a movement and applies its effect to the product's quantity (`IN` adds, `OUT` subtracts with a stock check, `ADJUSTMENT` sets the absolute value); also returns movement history for a product.
- Added `ReportService.totalStockValue()`, summing `price * quantity` across all products.
- Added `StockMovementRequest` DTO for the movement-creation payload.
- Exposed `POST /api/products/{productId}/movements` (ADMIN only) and `GET /api/products/{productId}/movements` (authenticated) via `StockMovementController`.
- Exposed `GET /api/reports/stock-value` via `ReportController`.
- Added unit tests for `StockMovementService` and `ReportService`, and controller slice tests for both new controllers.
- Updated `ROADMAP.md` (Phase 6 items 1 and 2 checked off) and `PROGRESS.md`.

---

## Checklist

- [x] Code follows the project coding standards
- [x] Project builds successfully
- [x] Unit tests pass
- [x] Documentation updated (if needed)
- [x] No unnecessary files committed
- [x] Linked to the correct Issue

---

## Screenshots (Optional)

N/A

---

## Testing

```
- Ran 'mvnw checkstyle:check' — 0 violations.
- Ran 'mvnw test' (excluding the Postgres-backed context test, no local DB available) — all suites green.
- New unit tests cover StockMovementService (IN/OUT/ADJUSTMENT, insufficient-stock rejection, history lookup) and ReportService (stock value sum, zero-product case).
- New controller slice tests cover movement creation/listing and the stock-value endpoint.
```

---

## Additional Notes

`OUT` movements are rejected with a 409 if they would take a product's quantity below zero. Movement history and stock-value reporting are read-only for any authenticated user; recording a movement requires ADMIN, matching the existing write-endpoint convention.
