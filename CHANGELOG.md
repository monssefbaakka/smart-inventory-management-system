# Changelog

All notable changes to the Smart Inventory Management System are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Stocktake / cycle counting** — count a warehouse line by line with the variance against the
  expected quantity, then complete the count to apply every line as an adjustment through the shared
  stock-movement trail (#127).
- **Stock transfers between warehouses** — move goods from one location to another in a single call;
  both stock levels change by equal and opposite amounts, the product total is untouched, and both
  legs land in the movement history as `TRANSFER_OUT`/`TRANSFER_IN` (#125).
- **Multi-warehouse/location stock tracking** — warehouse CRUD, per-warehouse stock levels, and stock
  movements that apply to a named location as well as the product total (#123).
- **Barcode/QR scanning support** — optional unique `barcode` on products, scan lookup endpoint, and
  on-demand Code 128 / QR label rendering as PNG (#121).

## [1.0.0] - 2026-07-23

First stable release. Full inventory management platform on Spring Boot 3.x / Java 17.

### Added

- **Product & category management** — validated CRUD REST endpoints.
- **Supplier management** — supplier CRUD endpoints (#27).
- **Purchase orders** — raise supplier orders with line items and drive their lifecycle
  (draft → placed → received); received goods flow through the stock-movement audit trail (#87).
- **Real-time stock tracking** — stock-movement records with a full audit trail.
- **Low-stock alerts** — configurable reorder thresholds with alert reporting (#77).
- **Reporting** — stock value and stock-movement history reports (#80).
- **CSV export** — downloadable product inventory and stock-movement history (#84).
- **Dashboard** — summary of counts, low-stock items, and recent activity (#82).
- **Spring Security baseline** — secured endpoints with a stateless security config (#29).
- **JWT authentication** — registration/login issuing JWTs; token filter and user details service (#30).
- **Role-based authorization** — `@PreAuthorize` role checks across controllers (#31).
- **Swagger / OpenAPI 3** — interactive API docs with a JWT **Authorize** button (#28).
- **Flyway migrations** — versioned database schema evolution.
- **Test suite** — unit and slice tests with JaCoCo coverage gates (#32).
- **Docker** — multi-stage Dockerfile and `.dockerignore` (#33).
- **Docker Compose** — app + PostgreSQL orchestration with health checks and volume persistence (#34).
- **CI pipeline** — GitHub Actions running build, Checkstyle, and tests with coverage gates (#35).

[Unreleased]: https://github.com/monssefbaakka/smart-inventory-management-system/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/monssefbaakka/smart-inventory-management-system/releases/tag/v1.0.0
