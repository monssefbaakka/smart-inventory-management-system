# Development Roadmap

This document outlines the planned development phases, milestones, and features for the Smart Inventory Management System.

## Overview

Smart Inventory Management System is a Spring Boot 4 (Java 17) backend for tracking and managing inventory, built on PostgreSQL, JPA, Flyway migrations, and Spring Security.

## Phase 1 — Foundation (complete)

- [x] Maven project scaffolding with Spring Boot
- [x] Logging configuration (Logback, level overrides)
- [x] Code style: Checkstyle, EditorConfig, coding standards
- [x] LICENSE and repository documentation
- [x] Development roadmap (this document)

## Phase 2 — Core Domain & Data Layer (complete)

- [x] Domain model: products, categories, suppliers, users, roles
- [x] JPA entities and repositories
- [x] Flyway migration baseline (schema versioning)
- [x] PostgreSQL local/dev environment setup (Docker Compose)

## Phase 3 — API & Business Logic (complete)

- [x] REST API for inventory CRUD (products, categories, suppliers)
- [ ] Stock movement tracking (inbound/outbound/adjustments) (deferred)
- [x] Validation rules (Bean Validation)
- [x] Global exception handling and consistent error responses

## Phase 4 — Security & Access Control (complete)

- [x] Authentication (Spring Security)
- [x] Role-based authorization (admin, manager, staff)
- [x] Secure endpoints per role

## Phase 5 — Quality & Testing (complete)

- [x] Unit tests (service/repository layers)
- [x] Integration tests (JPA, Flyway, Security test starters already in place)
- [x] CI pipeline (build, test, lint on PR)

## Phase 6 — Reporting & Insights (complete)

- [x] Low-stock alerts / reorder thresholds (per product, and per warehouse — #159, including a site
      emptied by a transfer — #161)
- [x] Inventory reports (stock value at retail and at cost, cost of goods sold, movement history)
- [x] Basic dashboard endpoints

## Milestones

| Milestone | Scope | Target |
|---|---|---|
| M1 — Project Bootstrap | Phase 1 complete | Done |
| M2 — Data Layer Ready | Phase 2 complete | Done |
| M3 — MVP API | Phase 3 complete | Done |
| M4 — Secured Release | Phase 4 complete | Done |
| M5 — Tested Release | Phase 5 complete | Done |
| M6 — Reporting Release | Phase 6 complete | Done |

## Main Features (Planned)

- Product & category management
- Multi-tenant isolation of all inventory data (done — #129)
- Multi-warehouse/location stock tracking (done — #123)
- Stock transfers between warehouses (done — #125; a transfer reorders for the site it emptied — #161)
- Stocktake / cycle counting with variance reporting (done — #127)
- Stock movement history (in/out/adjustments)
- Stock reservations with available-to-promise (done — #146)
- Weighted-average inventory costing, valuation at cost and cost of goods sold (done — #148)
- Role-based access control
- Low-stock alerts and reorder thresholds
- Inventory reporting

## Future Improvements

- Frontend client (web UI)
- Barcode/QR scanning support (done — #121)
- Supplier and purchase-order management (purchase-order management done — #87; partial goods
  receipts done — #150; receipts into a warehouse and a lot done — #153; a delivery warehouse on the
  order done — #155; a default delivery warehouse on the supplier done — #157)
- Notifications (email/webhook) for stock events
- Multi-tenant support (done — #129)
- Export to CSV/Excel/PDF (CSV product + stock-movement export done — #84; Excel/PDF still open)
- API rate limiting and audit logging (audit logging done — #117; rate limiting done — #119)

## Notes

Dates are TBD pending team capacity planning; this roadmap will be updated as phases complete.
