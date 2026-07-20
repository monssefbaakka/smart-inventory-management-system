# Pull Request

## Summary

This Pull Request implements the CSV slice of the ROADMAP Future Improvement "Export to CSV/Excel/PDF": downloadable CSV exports for products and stock movement history, reusing the existing `ReportController`/`ReportService`.

---

## Related Issue

Closes #84

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

- Added `ReportService.exportProductsToCsv()`: builds a CSV document (id, sku, name, category, quantity, price, stockValue) from all products, with proper quoting/escaping of commas, quotes, and newlines.
- Added `ReportService.exportStockMovementsToCsv()`: builds a CSV document (id, productId, productSku, type, quantity, note, createdAt) from all stock movements, most recent first.
- Added `GET /api/reports/export/products` and `GET /api/reports/export/movements` on `ReportController`, both returning `text/csv` with a `Content-Disposition: attachment` header; extracted a shared `csvAttachment(csv, filename)` helper to avoid duplicating the response-building logic between the two endpoints.
- Declared the CSV `Content-Type` with an explicit UTF-8 charset (`text/csv;charset=UTF-8`) since the body is encoded as UTF-8, avoiding client-side mis-decoding.
- Added unit tests for both export methods (empty input, single/multiple rows in repository order, missing category, zero quantity, null note, comma/quote escaping in product names and movement notes) and controller slice tests for both endpoints, including the charset header.
- Updated `ROADMAP.md` (CSV export checked off under Future Improvements), `PROGRESS.md` (issue #84 tracked; also corrected #80's stale "working" status to "done"), and `README.md` (new CSV Export feature bullet).

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
- Ran 'mvnw -Dtest=ReportServiceTest,ReportControllerTest test' — all green.
- New unit tests cover ReportService (products CSV: empty, single row, multi-row order, missing category,
  zero quantity, comma/quote escaping; movements CSV: empty, single row, multi-row order, null note,
  comma escaping in note) and ReportController (both export endpoints return text/csv;charset=UTF-8 with
  the expected Content-Disposition and body).
```

---

## Additional Notes

Both export endpoints are read-only and available to any authenticated user, matching the existing convention for the rest of `ReportController`. Excel and PDF export formats remain open items under ROADMAP Future Improvements.
