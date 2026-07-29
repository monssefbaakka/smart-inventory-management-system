# Pull Request

## Summary

This Pull Request implements the ROADMAP Future Improvement "Multi-tenant support": a single deployment can now serve several organisations with fully isolated inventory data. Every tenant-owned table carries a `tenant_id` discriminator and Hibernate's discriminator-based multi-tenancy stamps it on insert and appends it to every query, so isolation is enforced in the persistence layer rather than by each service remembering to filter.

---

## Related Issue

Closes #129

---

## Type of Change

- [x] Feature
- [ ] Bug Fix
- [ ] Refactoring
- [x] Documentation
- [x] Test
- [ ] Performance Improvement
- [ ] CI/CD
- [ ] Build

---

## What Changed

- Added the `Tenant` entity (slug, name, active flag, timestamps) with `TenantRepository`, `TenantService` (create, read, update, `findActiveBySlug`) and an ADMIN-only `TenantController` exposing `POST /api/tenants`, `GET /api/tenants`, `GET /api/tenants/{id}` and `PUT /api/tenants/{id}`, plus `GET /api/tenants/current` returning the calling account's own tenant. The slug is immutable on update, since it is stamped on every row the tenant owns.
- Added `V11__multi_tenancy.sql`: creates the `tenants` table, seeds a `default` tenant, adds `tenant_id` to `users`, `categories`, `suppliers`, `products`, `warehouses`, `stock_levels`, `stock_movements`, `stock_transfers`, `stock_counts`, `purchase_orders` and `audit_logs`, backfills existing rows to `default`, then sets each column `NOT NULL` with a foreign key to `tenants (slug)` and an index.
- Rewrote the business-key constraints so they are unique *within* a tenant instead of across the installation: `(tenant_id, sku)` and `(tenant_id, barcode)` on products, `(tenant_id, name)` on categories, `(tenant_id, email)` on suppliers, `(tenant_id, code)` on warehouses. Two tenants may now both hold SKU `SKU-1`.
- Annotated every tenant-owned entity with Hibernate's `@TenantId` and registered a `TenantIdentifierResolver` through a `HibernatePropertiesCustomizer` in the new `MultiTenancyConfig`, so the discriminator is applied to inserts and queries automatically.
- Added `TenantContext` (a thread-local holding the current tenant) and `TenantFilter`, which binds the authenticated caller's tenant for the request and unbinds it in a `finally` block so pooled threads never leak it. The filter is registered in the security chain after authentication, with its plain servlet-chain registration disabled — mirroring `RateLimitFilter`, because a once-per-request filter running before authentication would otherwise suppress the instance that can actually see the principal.
- Added `AuthenticatedUser`, a `UserDetails` carrying the account's tenant, and returned it from `UserDetailsServiceImpl` so the tenant travels with the authenticated principal.
- Gave `User` a plain `tenant_id` column rather than the `@TenantId` discriminator: accounts are looked up by email during login, before any tenant is known. Email therefore stays globally unique and identifies both the user and the tenant it belongs to.
- Extended `RegisterRequest` with an optional `tenantSlug` and reworked `AuthService` to resolve it through `TenantService.findActiveBySlug()`, falling back to the configurable default tenant when the payload names none.
- Added `DuplicateTenantSlugException` (409) and `InactiveTenantException` (400) with handlers in `GlobalExceptionHandler`.
- Added the `multitenancy.default-tenant` property (`DEFAULT_TENANT`, default `default`), used for registrations naming no tenant and for work running outside a request such as startup or scheduled jobs.
- Added unit tests for the tenant registry, thread-local context, resolver fallback, request filter (binding, clearing on success and on failure, anonymous and tenant-less principals) and the tenant endpoints; updated the auth, user-details and exception-handler tests for the tenant-aware behaviour.
- Updated `README.md` (new Multi-Tenancy feature bullet and section), `ROADMAP.md` (multi-tenant support checked off), `CHANGELOG.md` (Unreleased entry) and `PROGRESS.md` (issue #129 tracked; #127 moved from "working" to "done").

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
- Ran 'mvnw clean verify' — 316 tests pass, 0 Checkstyle violations, JaCoCo 80% line gate met.
- Ran the application against PostgreSQL: Flyway applied all 11 migrations, including V11 on an
  already-populated schema.
- Manual isolation check with two tenants ('default' and 'acme'), one account each:
  both created a category named 'Tools' and a product with SKU 'SKU-1' — previously globally
  unique — and both succeeded; each account's list endpoints returned only its own rows;
  reading the other tenant's product or category by id returned 404; GET /api/tenants/current
  returned the correct slug for each account.
```

---

## Additional Notes

Child tables (`purchase_order_items`, `stock_count_lines`) deliberately carry no discriminator: they are only reachable through an already-scoped parent.

The tenant registry spans the whole installation rather than a single tenant, so managing it is an ADMIN-only operation; every other endpoint only ever sees the caller's own tenant.

Unrelated pre-existing defect found while exercising the API against a real database: `GET /api/categories` and `GET /api/suppliers` answer `500 Failed to write request` whenever rows exist, because both entities expose a lazy `@OneToMany` products collection and are serialised outside a transaction (`spring.jpa.open-in-view=false`). Nothing in this PR changes fetch or serialisation behaviour, so it is left for a separate fix.
