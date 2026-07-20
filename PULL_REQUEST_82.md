# Pull Request

## Summary

This Pull Request implements the basic dashboard endpoints from Phase 6 of the roadmap: an aggregate summary (entity counts, low-stock count, total stock value), a recent stock-movement activity feed, and a low-stock product list, so a frontend can render a dashboard view without composing multiple report/report-adjacent calls.

---

## Related Issue

Closes #82

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

- Added `DashboardSummaryResponse` DTO carrying total products/categories/suppliers, low-stock count, and total stock value.
- Added `StockMovementRepository.findAllByOrderByCreatedAtDesc(Pageable)` to fetch the most recent movements across all products.
- Added `DashboardService`:
  - `summary()` — aggregates counts from `ProductRepository`, `CategoryRepository`, `SupplierRepository`, the low-stock query, and `ReportService.totalStockValue()`.
  - `recentMovements(limit)` — returns the most recent stock movements, clamping `limit` to `[1, 100]` to guard against invalid or excessive page sizes.
  - `lowStockProducts()` — convenience delegate to the existing low-stock query.
- Added `DashboardController` exposing:
  - `GET /api/dashboard/summary`
  - `GET /api/dashboard/recent-movements?limit=` (default 10)
  - `GET /api/dashboard/low-stock`
- Added unit tests for `DashboardService` (summary aggregation, zero-low-stock case, limit clamping at both bounds, low-stock delegation) and controller slice tests for all three endpoints.
- Updated `ROADMAP.md` (Phase 6 fully checked off, M6 marked Done) and `PROGRESS.md` (issue #82 tracked, #80 marked done).

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
- Ran 'mvnw -Dtest=DashboardServiceTest,DashboardControllerTest,ReportServiceTest,ReportControllerTest,StockMovementServiceTest,StockMovementControllerTest test' — all green.
- New unit tests cover DashboardService (summary aggregation, zero-low-stock case, recentMovements limit clamping at both the low and high bound, lowStockProducts delegation) and DashboardController (summary, recent-movements default/explicit limit, low-stock).
```

---

## Additional Notes

All three dashboard endpoints are read-only and available to any authenticated user, matching the existing convention for `ReportController`. `recentMovements` clamps its `limit` parameter server-side rather than rejecting out-of-range values, keeping the endpoint permissive for simple frontend usage.
