# Pull Request

## Summary

This Pull Request brings low-stock alert functionality onto `main`. The feature was originally implemented and merged via PR #78, but that PR targeted the `issue-36-release-v1.0` branch instead of `main`, so it never actually landed. This PR cherry-picks the original commits onto `main` and fixes a validation bug uncovered while doing so.

---

## Related Issue

Closes #77

---

## Type of Change

- [x] Feature
- [x] Bug Fix
- [ ] Refactoring
- [ ] Documentation
- [ ] Performance Improvement
- [x] Test
- [ ] CI/CD
- [ ] Build

---

## What Changed

- Added a `reorderThreshold` field (`INTEGER NOT NULL DEFAULT 10`) to the `Product` entity via Flyway migration `V3__add_reorder_threshold.sql`.
- Added `findLowStockProducts()` to `ProductRepository` and `ProductService`, selecting products where `quantity <= reorderThreshold`.
- Exposed `GET /api/products/low-stock`.
- Added service and controller test coverage for the new behavior.
- **Fix:** `@Builder.Default` only seeds `reorderThreshold` through the builder — Jackson deserializes JSON payloads through the no-args constructor, leaving the field `null` and failing `@NotNull` on create/update. Removed `@NotNull` from the field, default it in `@PrePersist` when absent, and only overwrite it on update when explicitly provided in the request body. This was caught by the pre-existing `createReturnsCreatedProduct` / `updateReturnsUpdatedProduct` tests, which don't send the field.

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
```

---

## Additional Notes

Issue #77 was mistakenly closed earlier in this session before this gap was discovered, then reopened once it was clear the feature wasn't actually on `main`.
