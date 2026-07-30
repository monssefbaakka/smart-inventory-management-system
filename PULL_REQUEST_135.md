# Pull Request

## Summary

This Pull Request fixes the `500 Failed to write request` returned by several endpoints as soon as any row existed. The application runs with `spring.jpa.open-in-view=false`, so the persistence session is already closed when a response is serialised; controllers that handed a JPA entity straight to Jackson therefore failed on the first lazy association it reached. Responses are now flat DTOs assembled while the data is still loadable, and the finders behind them fetch the associations those DTOs read.

---

## Related Issue

Closes #135

---

## Type of Change

- [ ] Feature
- [x] Bug Fix
- [x] Refactoring
- [x] Documentation
- [x] Test
- [ ] Performance Improvement
- [ ] CI/CD
- [ ] Build

---

## What Changed

- Added six response DTOs following the existing `StockLevelResponse` / `StockTransferResponse` pattern — flat records with a `from(...)` factory and OpenAPI `@Schema` annotations: `CategoryResponse`, `SupplierResponse`, `ProductResponse`, `StockMovementResponse`, `PurchaseOrderResponse` and `PurchaseOrderItemResponse`.
- Switched `CategoryController`, `SupplierController`, `ProductController`, `StockMovementController`, `PurchaseOrderController` and `DashboardController` to return those DTOs. Request bodies are unchanged, so the endpoints still accept exactly what they accepted before.
- Related records are flattened to an id and a label instead of being nested whole: a product carries `categoryId`/`categoryName` and `supplierId`/`supplierName`, a movement carries `productId`/`sku`/`productName` and `warehouseId`/`warehouseCode`, an order carries `supplierId`/`supplierName` and its line items. Category and supplier responses drop their `products` collection entirely — it is a lazy association reachable through `/api/products` instead.
- Added `@EntityGraph` fetch plans to the finders whose results are rendered: `category` and `supplier` on `ProductRepository`, `product` and `warehouse` on `StockMovementRepository`, `supplier`, `items` and `items.product` on `PurchaseOrderRepository`. This is what guarantees no proxy escapes to the serialiser; it also collapses the per-row queries those listings previously issued.
- `ProductService.create` and `update` now resolve the category and supplier named in the payload through `CategoryService` / `SupplierService` before saving. The saved product holds a managed reference, so its response carries the real category and supplier names, and naming an id that does not exist is answered with `404 Not Found` instead of a foreign-key violation. A payload naming no category, or one without an id, leaves the product unattached.
- Responses no longer carry the `tenant_id` discriminator. It is a persistence detail of the multi-tenancy scheme and every caller only ever sees its own tenant anyway.
- Added tests for the flattened payloads on every affected controller, including assertions that the nested entity fields and `tenantId` are gone, and unit tests for the new association resolution: persisted references replacing requested ones, an unnamed association staying null without touching the services, and an unknown id aborting the save.
- Updated `README.md` (new "Response Shape" section), `CHANGELOG.md` (Unreleased → Fixed) and `PROGRESS.md` (issue #135 tracked; #129 moved from "working" to "done").

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

Before, against a populated database:

```
GET /api/categories
{"status":500,"error":"Internal Server Error","message":"Failed to write request"}
```

After:

```
GET /api/products/3
{
  "id": 3, "name": "Widget-135", "sku": "SKU-135", "price": 19.99, "quantity": 42,
  "reorderThreshold": 10,
  "categoryId": 3, "categoryName": "Tools-135",
  "supplierId": 2, "supplierName": "Acme-135"
}
```

---

## Testing

```
- Ran 'mvnw clean verify' — 324 tests pass, 0 Checkstyle violations, JaCoCo 80% line gate met.
- Ran the application against PostgreSQL and exercised every endpoint named in the issue on a
  database that already held rows:
  - GET /api/categories and GET /api/suppliers, both previously 500, now 200.
  - POST /api/products with a category and supplier returns 201 carrying categoryName and
    supplierName; GET /api/products and GET /api/products/{id} return 200.
  - POST /api/products naming a category id that does not exist returns 404.
  - PUT /api/products omitting the category clears it and returns 200.
  - POST and GET /api/products/{id}/movements, GET /api/dashboard/recent-movements and
    GET /api/dashboard/low-stock all return 200 with flattened payloads.
  - POST, GET, place and receive on /api/purchase-orders return the order with its supplier,
    line items and total.
  - No LazyInitializationException in the application log across the whole run.
```

---

## Additional Notes

The entity-returning service methods are untouched: `ProductService.findById` and `SupplierService.findById` are called by `PurchaseOrderService`, `StockCountService`, `StockLevelService`, `StockMovementService` and `StockTransferService`, which need the entity rather than a payload. Only the controllers changed shape.

`Warehouse` and `AuditLog` have no lazy associations, so `WarehouseController` and `AuditLogController` were left alone rather than converted for symmetry.
