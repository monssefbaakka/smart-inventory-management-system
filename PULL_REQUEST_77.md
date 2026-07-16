# Pull Request

## Summary

This Pull Request implements low-stock alert functionality by introducing a configurable reorder threshold on the `Product` entity. Products whose current stock quantity falls at or below their threshold are surfaced via a dedicated API endpoint, giving operators a clear view of items requiring restocking.

---

## Related Issue

Closes #77

---

## Type of Change

- [x] Feature
- [ ] Bug Fix
- [ ] Refactoring
- [ ] Documentation
- [ ] Performance Improvement
- [x] Test
- [ ] CI/CD
- [x] Build

---

## What Changed

- Added a `reorderThreshold` field (`INTEGER NOT NULL DEFAULT 10`) to the `Product` entity with a `@Builder.Default` value of `10`, ensuring backward compatibility with existing tests.
- Added Flyway migration `V3__add_reorder_threshold.sql` to backfill the new column on the `products` table.
- Added `findLowStockProducts()` JPQL query to `ProductRepository` selecting products where `quantity <= reorderThreshold`.
- Extended `ProductService` with a `findLowStockProducts()` method and updated `update()` to persist the `reorderThreshold` field.
- Exposed `GET /api/products/low-stock` in `ProductController` (accessible to authenticated users).
- Added service-layer unit tests (`findLowStockProductsReturnsBelowThreshold`, `updateSetsReorderThreshold`) and a controller slice test (`findLowStockReturnsOkWithProducts`).
- Updated `PROGRESS.md` to mark Issue #36 as done and Issue #77 as working.

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

Describe how this change was tested.

```
- Ran 'mvnw checkstyle:check' — 0 violations.
- Ran 'mvnw clean package -DskipTests' — BUILD SUCCESS (version 1.0.0).
- New unit tests cover service and controller layers for the low-stock path.
```

---

## Additional Notes

The reorder threshold defaults to 10 if not specified during product creation. The value is fully updatable via the existing `PUT /api/products/{id}` endpoint.
