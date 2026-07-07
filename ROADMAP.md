# Development Roadmap

This document outlines the planned development phases, milestones, and features for the Smart Inventory Management System.

## Overview

Smart Inventory Management System is a Spring Boot 4 (Java 17) backend for tracking and managing inventory, built on PostgreSQL, JPA, Flyway migrations, and Spring Security.

## Phase 1 — Foundation (current)

- [x] Maven project scaffolding with Spring Boot
- [x] Logging configuration (Logback, level overrides)
- [x] Code style: Checkstyle, EditorConfig, coding standards
- [ ] LICENSE and repository documentation
- [ ] Development roadmap (this document)

## Phase 2 — Core Domain & Data Layer

- [ ] Domain model: products, categories, warehouses/locations, stock levels
- [ ] JPA entities and repositories
- [ ] Flyway migration baseline (schema versioning)
- [ ] PostgreSQL local/dev environment setup (Docker Compose)

## Phase 3 — API & Business Logic

- [ ] REST API for inventory CRUD (products, stock, categories)
- [ ] Stock movement tracking (inbound/outbound/adjustments)
- [ ] Validation rules (Bean Validation)
- [ ] Global exception handling and consistent error responses

## Phase 4 — Security & Access Control

- [ ] Authentication (Spring Security)
- [ ] Role-based authorization (admin, manager, staff)
- [ ] Secure endpoints per role

## Phase 5 — Quality & Testing

- [ ] Unit tests (service/repository layers)
- [ ] Integration tests (JPA, Flyway, Security test starters already in place)
- [ ] CI pipeline (build, test, lint on PR)

## Phase 6 — Reporting & Insights

- [ ] Low-stock alerts / reorder thresholds
- [ ] Inventory reports (stock value, movement history)
- [ ] Basic dashboard endpoints

## Milestones

| Milestone | Scope | Target |
|---|---|---|
| M1 — Project Bootstrap | Phase 1 complete | Done |
| M2 — Data Layer Ready | Phase 2 complete | TBD |
| M3 — MVP API | Phase 3 complete | TBD |
| M4 — Secured Release | Phase 4 complete | TBD |
| M5 — Tested Release | Phase 5 complete | TBD |
| M6 — Reporting Release | Phase 6 complete | TBD |

## Main Features (Planned)

- Product & category management
- Multi-warehouse/location stock tracking
- Stock movement history (in/out/adjustments)
- Role-based access control
- Low-stock alerts and reorder thresholds
- Inventory reporting

## Future Improvements

- Frontend client (web UI)
- Barcode/QR scanning support
- Supplier and purchase-order management
- Notifications (email/webhook) for stock events
- Multi-tenant support
- Export to CSV/Excel/PDF
- API rate limiting and audit logging

## Notes

Dates are TBD pending team capacity planning; this roadmap will be updated as phases complete.
