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

- [x] Low-stock alerts / reorder thresholds
- [x] Inventory reports (stock value, movement history)
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
