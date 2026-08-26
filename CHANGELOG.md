# Changelog

All notable changes to the Smart Inventory Management System are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **An order records the day its goods turned up** — the receipt that completes a purchase order
  stamps it with a `deliveredDate`, reported on the order response. An order delivered in parts is
  dated by the day the last part landed: an order is not delivered until all of it is. Anything still
  outstanding means no date yet, and a cancelled order never gets one — what had already arrived
  stays in stock, but the order was abandoned rather than fulfilled. Unlike `overdue`, it is a record
  of an event rather than a figure worked out on reading, and is never recomputed once stamped.
  Orders received before the column existed keep no date, the day they arrived not being recoverable
  now. Nothing yet reads the promised and actual dates back together (#179).

- **Which orders are running late** — a purchase order reports `overdue`, and the listing takes
  `overdue=true` to keep only the orders that are, on its own or alongside `supplierId` and the
  paging and sorting it already had. Late means still being waited on — `PLACED` or
  `PARTIALLY_RECEIVED` — with an expected delivery date in the past; a draft nobody has sent, an
  order received or cancelled, and one carrying no date are none of them late, and an order due
  today has the whole of the day it was promised for. The flag is worked out as the order is read
  and never stored, the way a lot's expiry is: an order due on the eighth is fine on the seventh and
  late on the ninth without anybody touching it. Nothing is notified or escalated on the strength of
  it (#177).

- **An order says when its goods are due** — a supplier may name a `leadTimeDays`, how long they take
  between an order reaching them and the goods arriving, and a purchase order carries an
  `expectedDeliveryDate`. The date is stamped when the order is placed rather than when it is
  drafted: a lead time is counted from the day the order reaches the supplier, and a draft raised
  automatically may sit for days before a buyer sends it. A date the buyer named when raising the
  order is kept as given, and an order whose supplier names no lead time is placed without a date
  rather than with a guessed one. Read once, at placing, so revising a supplier's lead time later
  moves the orders placed from then on and not the ones already out with them. Both fields are set
  and read through the ordinary supplier and purchase-order endpoints and reported on their
  responses. Nothing about reordering changes: an order outstanding is cover whether it is due
  tomorrow or next quarter (#175).

- **Orders are never raised below the supplier's minimum** — a product may name a
  `minimumOrderQuantity`, the fewest units its supplier will accept, and the automatic reorder lifts
  a smaller quantity up to it. A shortfall of seventeen against a minimum of a hundred is ordered as
  a hundred, and a configured `reorderQuantity` of fifty is lifted the same way. The minimum is
  applied before the `packSize` rounding added in #171, so a minimum of a hundred against a pack of
  twelve is ordered as a hundred and eight rather than as a whole number of packs the supplier still
  refuses; the note on the order records both adjustments in the order they were made. Like pack
  size, the minimum decides how much, never whether — the comparison of free stock plus incoming
  against the reorder point is made before it. Left unset the rule orders exactly what it did before,
  and purchase orders entered by hand are never adjusted (#173).

- **Products can be ordered in whole packs** — a product may name a `packSize`, the number of units
  its supplier ships together, and the automatic reorder rounds the quantity it arrives at up to a
  whole multiple of it. A shortfall of seventeen against a pack of twelve is ordered as twenty-four,
  and a configured `reorderQuantity` of fifty is ordered as sixty; the note on the order records the
  rounding. Up, never to nearest, so the rounded order still covers the shortfall that raised it.
  Pack size decides how much, never whether — the comparison of free stock plus incoming against the
  reorder point is made before any rounding. Left unset the rule orders exactly what it did before,
  and purchase orders entered by hand are never rounded (#171).

### Changed

- **Low stock is measured on what is free, not on what is on the shelf** — the automatic reorder and
  the notification channels now measure stock on hand less what unlapsed reservations are holding,
  floored at zero, counted in the scope being measured. A product holding forty with thirty-eight
  promised is measured as two. Taking a hold and giving one back are measured on the same terms a
  movement is, so a shelf promised away notifies and reorders when the promise is made rather than
  when the goods are collected; fulfilment still measures once, through the `OUT` movement it
  records. The notification payload gains `reserved`, and `quantity` is now the free figure — on hand
  is the two added together (#167).

- **A shortage is announced once, not on every movement that keeps it going** — the notification
  channels now dispatch on a change of condition rather than on every measurement. Each measured
  shelf remembers what it last announced (the product total on the product, a warehouse measured
  against its own reorder point on its stock level), so a repeat is silent, a shortage that deepens
  to `OUT_OF_STOCK` is announced again, a partial restock that leaves the shelf low lowers what
  stands without announcing, and recovery above the threshold clears it so the next fall is announced
  afresh. Recipients that relied on an alert per movement now receive one per state change (#165).

### Fixed

- **REST responses no longer fail on lazy associations** — products, categories, suppliers, stock
  movements and purchase orders are returned as flat response DTOs instead of JPA entities, so
  endpoints no longer answer `500 Failed to write request` when Jackson reaches an uninitialised
  proxy under `spring.jpa.open-in-view=false`. The payloads also stop leaking the `tenant_id`
  discriminator, and a product naming a category or supplier that does not exist now gets `404`
  instead of a constraint violation (#135).

### Changed

- **The history listings return a page, not the whole table** — `GET /api/products/{id}/movements`,
  `GET /api/stock-transfers`, `GET /api/stock-counts`, `GET /api/purchase-orders` and
  `GET /api/audit-logs` now answer with the same envelope as the product listing, ordered
  `createdAt,desc` by default, and take `page`, `size` and `sort`. Callers reading the array directly
  must read `content`. Their existing filters are unchanged (#140).
- **`GET /api/audit-logs` returns a DTO** — audit entries come back as `AuditLogResponse` instead of
  the `AuditLog` entity, so the payload no longer carries the `tenant_id` discriminator (#140).
- **`GET /api/products` returns a page, not the whole catalogue** — the response is now an envelope
  carrying `content` plus `page`, `size`, `totalElements`, `totalPages`, `first` and `last`, instead
  of a bare JSON array. Callers reading the array directly must read `content`. Page size defaults to
  20 and is capped at 100 (#138).

### Added

- **Low-stock alerts name the site that ran low** — a movement through a warehouse that holds its own
  reorder point for the product is now classified against that site: its own quantity, against its own
  threshold. The notification carries `warehouseId` and `warehouseCode`, the log line and the email
  subject and body name the location, and a transfer that drops the source site to or below its own
  reorder point alerts for it as a sale does. Previously only the product total was measured, so a
  branch down to its last two units was announced to nobody while the group still looked comfortable.
  A movement through a warehouse holding no reorder point of its own, or through none at all, is
  classified on the product total exactly as before and both new fields are `null` (#163).
- **A stock transfer reorders for the site it emptied** — a transfer that leaves the source warehouse
  at or below the reorder point that warehouse holds for the product now raises a draft order for
  that site, sized for it and delivered to it, on the same terms a stock movement does. Stock is
  pulled to where it is selling, so a transfer is the common way for a site to run down, and the site
  left behind was previously the one nobody was watching. Only the site rule applies: a source
  warehouse naming no reorder point of its own raises nothing, because the only other figure to
  measure is the product total and a transfer leaves it unchanged. The destination is never
  evaluated, the one-order-per-shortfall rule is still read per site, and the rule still sits behind
  `auto-reorder.enabled` (#161).
- **A warehouse carries its own reorder point for a product** — `PUT
  /api/products/{id}/stock/{warehouseId}/reorder-threshold` records how low one site may get before
  it orders for itself, reported back on the level as `reorderThreshold`. A movement through a
  warehouse holding one is measured against that site alone rather than against the product total, so
  a depot down to its last two units raises an order even while the group looks comfortable. The
  order is delivered to that site, whatever the supplier's default says, and sized to bring it back
  to twice its own threshold unless the product sets a `reorderQuantity`. The one-order-per-shortfall
  rule is read per site: an open order already heading there raises nothing, one heading elsewhere
  does not stop it. A site the product has never been stocked in may be given a threshold, creating
  the level at zero units; `{"reorderThreshold": null}` clears it. A warehouse naming no threshold,
  and a movement recorded without one, are judged against the product total exactly as before, and
  the low-stock notification channels still classify on the product total (#159).
- **A supplier carries the warehouse its goods are normally delivered to** — a supplier takes an
  optional `defaultWarehouse` (`{ "id": 1 }`, as a product names its category), reported back as
  `defaultWarehouseId` and `defaultWarehouseCode`. An order raised without a `warehouseId` of its
  own is delivered there, and so is one the automatic reorder rule raises from inside a stock
  movement, where nobody is present to name a destination — replenishment now lands in a location
  instead of against the product total only. The warehouse is read when the order is raised, so
  changing a supplier's default leaves orders already raised going where they were going, and naming
  a `warehouseId` on the order still wins. A `defaultWarehouse` no site carries answers `404 Not
  Found` when the supplier is saved. A supplier naming no default is unchanged (#157).
- **A purchase order carries the warehouse it is to be delivered to** — `POST /api/purchase-orders`
  takes an optional `warehouseId`, reported back as `warehouseId` and `warehouseCode`, and every
  receipt against the order books into it without repeating it. The existing overrides keep their
  precedence: a receipt line's `warehouseId` beats the receipt's, and the receipt's beats the
  order's. `POST /{id}/receive` without a `warehouseId` now receives into the order's warehouse
  rather than into no location at all. A `warehouseId` no site carries answers `404 Not Found` when
  the order is raised. An order naming none behaves exactly as before (#155).
- **Goods receipts land in a warehouse and a lot** — a receipt carries an optional `warehouseId`,
  overridable per line, and each line an optional `lotCode` and `expiryDate`, so purchased stock
  reaches the location and the lot it is physically in instead of only the product total. A lot code
  the product does not carry yet is created against it, held in the receiving warehouse; one it
  already carries is reused, and stating it under a different expiry date answers `409 Conflict`. An
  `expiryDate` without a `lotCode` answers `400 Bad Request`. A line may be listed more than once to
  split a delivery across lots or sites, and what it may receive in total is still what it has
  outstanding. `POST /{id}/receive` takes an optional `warehouseId` query parameter. Saying none of
  it books exactly as before (#153).
- **Partial goods receipts** — `POST /api/purchase-orders/{id}/receipts` books one delivery against
  an order, taking the stated quantity of each named line into stock at the line's unit price. Lines
  left out stay outstanding, the order sits in the new `PARTIALLY_RECEIVED` status until every line
  is complete, and each line reports `receivedQuantity` and `outstandingQuantity`. A line received
  past the quantity ordered answers `409 Conflict` and the whole delivery is rolled back.
  `POST /{id}/receive` still receives everything outstanding and now also closes out a part-delivered
  order, cancelling is allowed from `PARTIALLY_RECEIVED`, and the automatic reorder counts a
  part-delivered order as still open (#150).
- **Inventory costing** — a product carries a weighted average of what its stock cost, rolled forward
  by every receipt that states a `unitCost`, and every movement records what it was valued at.
  `GET /api/reports/valuation` reports the inventory at cost and `GET /api/reports/cogs` totals what
  the stock that left over a window cost. Receiving a purchase order takes the goods in at the line's
  unit price. `GET /api/reports/stock-value` and the CSV, Excel and PDF exports still value stock at
  the selling price (#148).
- **Stock reservations** — stock can be held against an outbound commitment so it stops counting as
  available, with `GET /api/products/{id}/availability` reporting on hand, reserved and available.
  A hold names what it is for, optionally a warehouse and an expiry; reserving more than is available
  answers `409 Conflict`. Fulfilling a reservation records the `OUT` movement for it through the
  ordinary movement trail, releasing gives the stock back without moving anything, and a lapsed hold
  stops holding stock the moment it expires (#146).
- **Automatic reordering** — a stock movement that leaves a product at or below its reorder threshold
  raises a `DRAFT` purchase order against the product's supplier, priced at the product's unit price
  and flagged `autoGenerated` on the purchase order response. How much is ordered comes from the new
  optional `reorderQuantity` on a product, defaulting to enough to reach twice the reorder threshold.
  A product already on an open order, or with no supplier, is skipped. Off by default; enable with
  `auto-reorder.enabled` (#142).
- **Batch/lot tracking with expiry dates** — a product's stock can be tracked as lots, each with its
  own lot code, optional expiry date, optional warehouse and quantity. Movements take an optional
  `batchId`; an `OUT` naming none is allocated across the product's lots earliest expiry first, and
  an `ADJUSTMENT` may not name one. New endpoints declare, read and delete lots and report stock
  expiring within a window or already expired. Products without lots are unaffected (#144).
- **Paging on every unbounded listing** — the five history endpoints share one implementation of the
  page-size cap, the sortable-field allowlist and the `400` responses with the product listing, each
  with its own set of sortable fields (#140).
- **Paging, sorting and filtering on the product listing** — `page`, `size` and `sort=field,dir` over
  an allowlist of product fields, plus optional `search` (name and SKU, case-insensitive),
  `categoryId`, `supplierId`, `minPrice`, `maxPrice` and `lowStock` filters that combine with AND. An
  unusable sort field, page or size is answered with `400 Bad Request` rather than reaching the
  database (#138).
- **Multi-tenant support** — one deployment serves many organisations: a tenant registry with
  ADMIN-only endpoints, a `tenant_id` discriminator on every tenant-owned table, Hibernate
  discriminator-based tenancy driven by the authenticated caller's tenant, and registration into a
  named tenant (#129).
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
