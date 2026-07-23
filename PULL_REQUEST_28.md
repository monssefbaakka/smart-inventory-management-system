# Pull Request

## Summary

This Pull Request completes the Swagger/OpenAPI documentation for the REST API. The existing setup only registered a bare title and description; this change turns the Swagger UI into a usable, interactive reference: every endpoint now carries a summary, description and documented response codes, request/response payloads are annotated with field descriptions and examples, and a JWT bearer security scheme adds an **Authorize** button so protected routes can be exercised directly from the UI.

---

## Related Issue

Closes #28

---

## Type of Change

- [ ] Feature
- [ ] Bug Fix
- [ ] Refactoring
- [x] Documentation
- [ ] Performance Improvement
- [x] Test
- [ ] CI/CD
- [ ] Build

---

## What Changed

- Enriched `OpenApiConfig`:
  - Expanded the API `Info` (contact, MIT license URL, `v1.0` version, and a description explaining the JWT login flow).
  - Declared a `bearerAuth` HTTP bearer (JWT) `SecurityScheme` under `Components` and applied it as a global `SecurityRequirement`, so the Swagger UI shows an **Authorize** button and sends `Authorization: Bearer <token>` on subsequent requests.
  - Added a relative server entry.
- Annotated every controller with `@Operation` and `@ApiResponses` describing each endpoint's summary, behaviour and possible status codes (`200/201/204`, plus `400/401/403/404/409` where applicable), and documented path/query parameters with `@Parameter`:
  - `ProductController`, `CategoryController`, `SupplierController`, `StockMovementController`, `PurchaseOrderController`, `DashboardController`, `ReportController`.
  - `AuthController` is marked public with `@SecurityRequirements` (empty) so its register/login operations are not shown as requiring a token.
- Documented the request/response DTOs with `@Schema` descriptions and examples: `LoginRequest`, `RegisterRequest`, `AuthResponse`, `StockMovementRequest`, `PurchaseOrderRequest`, `PurchaseOrderItemRequest`, `DashboardSummaryResponse`.
- Added springdoc settings to `application.properties` (Swagger UI path, method/alpha sorting, request-duration display, api-docs path).
- Added `OpenApiConfigTest` verifying the API info, the `bearerAuth` scheme (type/scheme/bearer format), the global security requirement, and that at least one server is declared.
- Updated `README.md` (new feature bullet and an "API Documentation (Swagger / OpenAPI)" section with URLs and the Authorize walkthrough) and `PROGRESS.md` (issue #28 marked working).

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
- Ran 'mvnw -Dtest=OpenApiConfigTest test' — all green (4 tests).
- Ran full 'mvnw test' — 115 of 116 tests pass. The only failure is the full-context
  SmartInventoryManagementSystemApplicationTests.contextLoads, which starts Flyway against
  PostgreSQL and errors locally with 'Connection to localhost:5432 refused'; it is
  environment-dependent and unrelated to this change (CI provides the database).
```

---

## Additional Notes

The documentation is entirely annotation- and config-driven — no runtime behaviour, endpoints, security rules or payloads changed. Swagger UI (`/swagger-ui.html`) and the OpenAPI JSON (`/v3/api-docs`) remain public via the existing `SecurityConfig` allow-list; the new `bearerAuth` requirement only affects how the docs render and how the UI attaches the token, not server-side authorization, which continues to be enforced by Spring Security and the `@PreAuthorize` rules.
