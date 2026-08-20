# Smart Inventory Management System

[![CI](https://github.com/monssefbaakka/smart-inventory-management-system/actions/workflows/ci.yml/badge.svg)](https://github.com/monssefbaakka/smart-inventory-management-system/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](CHANGELOG.md)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

A modern, robust, and automated Inventory Management System built on Spring Boot 3.x and Java 17, designed to streamline and automate stock control, tracking, and warehouse workflows.

---

## 🚀 Features

- **Real-Time Stock Tracking:** Accurate monitoring of inventory levels, locations, and status.
- **Role-Based Access Control:** Secure user permissions for administrators, inventory managers, and staff.
- **Automated Alerts:** Low stock alerts and notification triggers to prevent supply chain disruptions.
- **Flyway Migrations:** Consistent database schema evolution across environments.
- **RESTful API:** Clean, validated endpoints for integration with frontend and external systems.
- **Reporting & Dashboard:** Stock value/movement reports plus a dashboard summary of counts, low-stock items, and recent activity, with downloadable CSV export of the product inventory and stock-movement history.
- **Purchase Orders:** Raise supplier purchase orders with line items, the warehouse they are to be delivered to, and drive their lifecycle (draft → placed → received), receiving a delivery in full or line by line as it arrives, into the warehouse and the lot the goods actually landed in, with received goods flowing through the stock-movement audit trail.
- **Automatic Reordering:** Stock reaching its reorder threshold raises a draft purchase order against the product's supplier, delivered to the site that supplier's goods normally go to, so a buyer only reviews and places it. A warehouse holding a reorder point of its own is measured on its own stock and ordered for by name,
whether it was emptied by a sale or by a transfer to another site.
- **Interactive API Docs:** Swagger UI and an OpenAPI 3 specification document every endpoint, with a built-in JWT **Authorize** button for trying protected routes.
- **API Rate Limiting:** Per-caller request budget over `/api/**` that returns `429 Too Many Requests` once exhausted.
- **Multi-Warehouse Stock:** Track how much of each product sits in each stocking location, with movements applied to a named warehouse.
- **Stock Transfers:** Move goods between warehouses in one call; both locations change by equal and opposite amounts and the product's overall quantity stays put.
- **Stocktake / Cycle Counting:** Count a warehouse line by line, see the variance against what the system expected, then commit the whole count as one reconciliation.
- **Batch & Expiry Tracking:** Track stock in lots with their own expiry dates, consume them earliest-expiry-first, and report what is expiring soon or already expired.
- **Stock Reservations:** Hold stock against an outbound commitment so it stops counting as available, and read on hand, reserved and available side by side before promising anything.
- **Inventory Costing:** Stock carries a weighted average of what it cost, rolled forward by every receipt, so the inventory can be valued at cost and the goods that left can be priced as cost of goods sold.
- **Barcode & QR Support:** Products carry a scannable barcode, resolve by scan in a single lookup, and render printable Code 128 or QR labels as PNG.
- **Multi-Tenancy:** One deployment serves many organisations; every row carries its tenant and every query is scoped to the caller's tenant, so tenants never see each other's inventory.

---

## 🛠️ Technology Stack

| Technology | Purpose |
| :--- | :--- |
| **Java 17** | Core programming language |
| **Spring Boot 4.1.0** | Main framework (Web MVC, JPA, Security, Validation) |
| **PostgreSQL** | Primary relational database |
| **Flyway** | Database migration engine |
| **Lombok** | Boilerplate code reduction |
| **Maven** | Dependency management & build tool |

---

## 🏛️ Architecture & Project Structure

The project follows a standard **Layered (Three-Tier) Architecture** to ensure separation of concerns, scalability, and ease of testing.

```mermaid
graph TD
    Client[Client / UI] -->|REST APIs| Controller[Presentation Layer: Controllers]
    Controller -->|DTOs / Requests| Service[Business Logic Layer: Services]
    Service -->|Entities| Repository[Data Access Layer: Repositories]
    Repository -->|SQL Queries| DB[(PostgreSQL Database)]
```

### Folder & Package Layout

```text
smart-inventory-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/example/smartinventory/
│   │   │   ├── config/          # Configurations (Security, Spring MVC, CORS)
│   │   │   ├── controller/      # REST API Controllers (endpoints)
│   │   │   ├── dto/             # Data Transfer Objects (request/response models)
│   │   │   ├── exception/       # Custom exceptions & global handler
│   │   │   ├── model/           # JPA Entities (database mappings)
│   │   │   ├── repository/      # Spring Data JPA Repository interfaces
│   │   │   └── service/         # Business logic Services (interfaces & impls)
│   │   └── resources/
│   │       ├── db/migration/    # Flyway migration scripts (SQL)
│   │       └── application.properties
│   └── test/
│       └── java/com/example/smartinventory/  # Unit & Integration tests
```

---

## 🎯 Project Roadmap

```mermaid
gantt
    title Project Roadmap & Milestones
    dateFormat  YYYY-MM-DD
    section Phase 1
    Initialization & Project Setup :active, 2026-07-01, 7d
    section Phase 2
    DB Setup & Schema Migration : 2026-07-08, 7d
    section Phase 3
    Core Domain Services (Products/Stocks) : 2026-07-15, 14d
    section Phase 4
    Security & Authentication (RBAC) : 2026-07-29, 10d
    section Phase 5
    Alerts & Transaction History : 2026-08-08, 10d
    section Phase 6
    Advanced Analytics & Intelligent Reordering : 2026-08-18, 14d
```

- [x] **Phase 1: Project Initialization & Directory Layout** (Done)
- [ ] **Phase 2: Database Setup & Flyway Migration** (Upcoming)
  - Configure connection pool and profiles.
  - Establish base SQL tables (products, suppliers, users, transactions).
- [ ] **Phase 3: Core Domain Services**
  - Implement CRUD APIs for Products, Suppliers, and Inventory.
- [ ] **Phase 4: Security & Authentication (RBAC)**
  - JWT integration and Spring Security policies.
- [ ] **Phase 5: Alerts & Transaction History**
  - Implement email notifications/event streams for low stock items.
- [ ] **Phase 6: Advanced Analytics & Intelligent Reordering**
  - Predictive demand planning and automated ordering integrations.

---

## ⚙️ Getting Started

### Prerequisites

- Java JDK 17 or higher
- PostgreSQL (running locally or via Docker)
- Maven 3.8+ (or using the Maven Wrapper included)

### Building the Project

Run the following command to compile and build the package:

```bash
./mvnw clean install
```

### Running the Application

To start the application locally:

```bash
./mvnw spring-boot:run
```

### Rate Limiting

Requests to `/api/**` are throttled per caller — by authenticated username when a valid token is
present, otherwise by remote address. Each response carries `X-RateLimit-Limit` and
`X-RateLimit-Remaining`; once the budget is exhausted the API answers `429 Too Many Requests` with a
`Retry-After` header.

| Property | Environment variable | Default | Purpose |
| :--- | :--- | :--- | :--- |
| `rate-limit.enabled` | `RATE_LIMIT_ENABLED` | `true` | Switches rate limiting on or off |
| `rate-limit.requests` | `RATE_LIMIT_REQUESTS` | `100` | Requests allowed per caller per window |
| `rate-limit.window-seconds` | `RATE_LIMIT_WINDOW_SECONDS` | `60` | Length of the refill window in seconds |

Counters are held in memory, so each application instance enforces its own budget.

### Automatic Reordering

Every stock movement is checked against the product's `reorderThreshold`. When the movement leaves
the product at or below it, a `DRAFT` purchase order is raised against the product's supplier,
priced at the product's current unit price and flagged `autoGenerated` in the purchase order
response. Nothing is sent anywhere: the draft waits for a buyer to review, place and receive it
through the ordinary purchase order endpoints.

| Property | Environment variable | Default | Purpose |
| :--- | :--- | :--- | :--- |
| `auto-reorder.enabled` | `AUTO_REORDER_ENABLED` | `false` | Switches automatic reordering on or off |

How much is ordered comes from the product's optional `reorderQuantity`. Left unset, the order tops
stock back up to twice the reorder threshold — a product with a threshold of 10 sitting at 3 units is
ordered 17.

```json
PUT /api/products/1
{
  "name": "Widget",
  "sku": "SKU-1",
  "price": 19.99,
  "quantity": 3,
  "reorderThreshold": 10,
  "reorderQuantity": 50,
  "supplier": { "id": 2 }
}
```

The order is delivered to the supplier's `defaultWarehouse`. Nobody is present to name a destination
when a stock movement raises an order, so without one on the supplier the replenishment arrives
against the product total only — invisible to the location that ran short in the first place.

The rule deliberately raises at most one order per shortfall:

- A product already sitting on an open (`DRAFT`, `PLACED` or `PARTIALLY_RECEIVED`) purchase order is
  skipped, so a shortfall that develops over several movements still produces a single order, and a
  short delivery does not raise a second order for goods still on their way. Receiving that order in
  full ends the skip.
- A product with no supplier is skipped and logged — there is nobody to order from.

#### Reordering for one warehouse

A total spread across four sites says nothing about the one that has run out. A warehouse may
therefore hold a reorder point of its own for a product, and a movement through that warehouse is
measured against that site alone — its own quantity, against its own threshold — rather than against
the product total.

```json
PUT /api/products/1/stock/2/reorder-threshold
{
  "reorderThreshold": 5
}
```

The threshold is reported back on the level, alongside the quantity, by
`GET /api/products/{id}/stock` and `GET /api/warehouses/{id}/stock`. A site the product has never
been stocked in may still be given one: the level starts at zero units, which is how a new location
is set up to reorder before its first delivery rather than after its first shortage. Sending
`{"reorderThreshold": null}` clears it.

An order a site raises for itself is delivered to that site, whatever the supplier's default says,
and is sized for it — the product's `reorderQuantity` when it sets one, and otherwise enough to bring
that site back to twice its own threshold. The one-order-per-shortfall rule is read per site: an open
order already heading for that warehouse raises nothing, while one heading elsewhere, or to nowhere
in particular, does not stop a short site from ordering for itself.

A warehouse that names no threshold is not measured on its own, and neither is a movement recorded
without one — both are judged against the product total, exactly as before.

A stock transfer measures the site the stock left in the same way: an empty shelf is an empty shelf
whether the goods were sold or sent on to another branch, so a transfer that drops the source site to
or below its own reorder point raises an order for that site, sized for it and delivered to it. Only
the site rule applies there — a source warehouse holding no reorder point of its own raises nothing,
because the only other figure to measure is the product total and a transfer leaves it exactly where
it was. The destination is never evaluated: no site falls below its reorder point by receiving goods.

The alert channels follow the same rule. A movement or transfer measured against a site is announced
for that site — the notification carries the location it is about, so the recipient is told which
shelf to fill rather than being handed a group total and left to find the shortage:

```json
{
  "productId": 1,
  "sku": "SKU-1",
  "name": "Widget",
  "warehouseId": 2,
  "warehouseCode": "WH-NORTH",
  "quantity": 2,
  "reorderThreshold": 5,
  "eventType": "LOW_STOCK",
  "occurredAt": "2026-05-04T09:15:00Z"
}
```

A site is classified once, not twice: a warehouse measured against its own reorder point is not also
measured against the product total, so one movement produces one alert. A movement through a
warehouse holding no reorder point of its own, or through none at all, is classified on the product
total exactly as before, and `warehouseId` and `warehouseCode` come back `null`. The log line and the
email name the site when there is one, and the webhook payload gains the two fields.

#### Said once, not on every movement

What is announced is the change, not the measurement. Each measured shelf remembers the condition it
last announced — the product total on the product, a warehouse measured against its own reorder point
on that stock level — and a movement that finds the same condition again dispatches nothing. A
product that has been one unit short since Tuesday is one alert, not one alert per sale.

| What the movement finds | What the channels get |
| :--- | :--- |
| A condition worse than the one standing, including the first | Announced, and it becomes what stands |
| The same condition as the one standing | Nothing; it has already been said |
| Still low, but no longer empty | Nothing, and `LOW_STOCK` becomes what stands |
| Back above the threshold | Nothing; what stands is cleared and the next fall is announced afresh |

So a shelf that empties after being low is announced again — `OUT_OF_STOCK` is worse than the
`LOW_STOCK` that stands — and a shelf partly restocked and then emptied again is announced again too,
because the partial restock lowered what stands. Recovery itself is silent: there is no channel here
for good news.

Each shelf remembers on its own. One site falling quiet says nothing about another site or about the
product total, and a shortage standing on the total does not silence a site that has just reached its
own reorder point.

What stands is written in the transaction that moved the stock, so a rolled-back movement remembers
nothing and a restart does not re-announce a shortage that already stands. A channel that throws is
still logged and skipped, and the shelf still counts as announced — a dead endpoint is a channel
problem, not a reason to say it all again to the channels that are up.

### Warehouses & Stock Levels

Warehouses are stocking locations, each with a unique `code`. A stock movement may name a warehouse
(`warehouseId` in the movement payload): the movement is then applied to that warehouse's level *and*
to the product's overall quantity, so a product's total stays the sum of what the locations hold.
Movements recorded without a warehouse change the overall quantity only.

| Endpoint | Purpose |
| :--- | :--- |
| `POST/PUT/DELETE /api/warehouses` | Manage warehouses (ADMIN) |
| `GET /api/warehouses` · `GET /api/warehouses/{id}` | Read warehouses |
| `GET /api/warehouses/{id}/stock` | Everything stocked in one warehouse |
| `GET /api/products/{id}/stock` | One product's stock, broken down by warehouse |
| `PUT /api/products/{id}/stock/{warehouseId}/reorder-threshold` | What one warehouse may fall to before it reorders (ADMIN) |

An `OUT` movement against a warehouse is rejected with `409 Conflict` when that location holds too
little stock, even if the product has enough elsewhere.

### Stock Transfers

A transfer moves stock between two warehouses in a single call. It changes *where* the stock is, not
how much of it exists: the source level drops, the destination level rises by the same amount, and
the product's overall quantity is untouched. Both legs are written to the movement history as
`TRANSFER_OUT` and `TRANSFER_IN`, so every level change is still explained by a movement row.

| Endpoint | Purpose |
| :--- | :--- |
| `POST /api/stock-transfers` | Move stock between two warehouses (ADMIN) |
| `GET /api/stock-transfers` | Transfer history, most recent first |
| `GET /api/stock-transfers?productId={id}` | Transfers of one product |
| `GET /api/stock-transfers?warehouseId={id}` | Transfers into *or* out of one warehouse |
| `GET /api/stock-transfers/{id}` | A single transfer |

```json
POST /api/stock-transfers
{
  "productId": 1,
  "sourceWarehouseId": 1,
  "destinationWarehouseId": 2,
  "quantity": 10,
  "note": "Rebalancing after regional demand spike"
}
```

Transfers are rejected with `400 Bad Request` when both sides name the same warehouse or the
destination is inactive, and with `409 Conflict` when the source location holds too little. Moving
stock *out of* an inactive warehouse stays allowed, so a site being wound down can be drained.
`TRANSFER_IN` and `TRANSFER_OUT` are not accepted by the ordinary movement endpoint — they always
come in pairs and are written only by a transfer.

A transfer that leaves the source warehouse at or below the reorder point that warehouse holds for
the product raises a draft order for it, exactly as a movement out of it would; see
[Reordering for one warehouse](#reordering-for-one-warehouse). A source holding no reorder point of
its own raises nothing, since a transfer cannot move the product total.

### Stocktake / Cycle Counting

A stock count reconciles one warehouse against what the system believes it holds. Counts are entered
while the count is `DRAFT` and touch no stock; each line snapshots the expected quantity when it is
entered, so the variance (`counted - expected`) is visible before anything is committed. Completing
the count applies every line as an `ADJUSTMENT` against the counted warehouse, so the per-warehouse
level *and* the product's overall quantity settle on the counted figure, and each correction lands in
the movement history.

| Endpoint | Purpose |
| :--- | :--- |
| `POST /api/stock-counts` | Open a count against a warehouse (ADMIN) |
| `POST /api/stock-counts/{id}/lines` | Record what was found for one product (ADMIN) |
| `POST /api/stock-counts/{id}/complete` | Apply every line to stock and close the count (ADMIN) |
| `POST /api/stock-counts/{id}/cancel` | Abandon a draft count, leaving stock untouched (ADMIN) |
| `GET /api/stock-counts` · `?warehouseId=` · `?status=` | Count history, most recent first |
| `GET /api/stock-counts/{id}` | One count with its lines and variances |

```json
POST /api/stock-counts/3/lines
{
  "productId": 1,
  "countedQuantity": 38
}
```

Counting the same product twice replaces the earlier line — a recount is the normal case, not an
error. Lines may only be added while the count is `DRAFT`, and only a `DRAFT` count can be completed
or cancelled; anything else answers `409 Conflict`, as does completing a count with no lines.

### Batches & Expiry

A batch is a lot of one product: a quantity sharing a lot code and an expiry date, optionally held in
a named warehouse. A recall names a lot rather than a product, and a carton received in March is not
the same stock as one received in July, so lots are tracked separately from the product total.

| Endpoint | Purpose |
| :--- | :--- |
| `POST /api/products/{id}/batches` | Declare a lot of a product (ADMIN) |
| `GET /api/products/{id}/batches` | Every lot of a product, earliest expiry first |
| `GET /api/batches/{id}` | A single lot |
| `GET /api/batches/expiring?days=30` | Lots still holding stock that expire within the window |
| `GET /api/batches/expired` | Lots past their expiry date that still hold stock |
| `DELETE /api/batches/{id}` | Stop tracking an empty lot (ADMIN) |

A lot is declared empty and filled by movements, so what it holds is always explained by the movement
history — or it is created by the goods receipt that brings the lot in, which amounts to the same
thing (see [Purchase Orders & Goods Receipts](#purchase-orders--goods-receipts)):

```json
POST /api/products/1/batches
{
  "lotCode": "A-2291",
  "expiryDate": "2026-12-31",
  "warehouseId": 1
}
```

The movement payload takes an optional `batchId`:

- `IN` naming a lot adds to it; `OUT` naming a lot takes from it and is rejected with `409 Conflict`
  when the lot holds too little.
- `OUT` naming no lot is spread across the product's lots **earliest expiry first**, lots with no
  expiry date drawn on last. Naming a warehouse restricts the allocation to the stock held there.
  Availability is summed before anything is deducted, so a movement asking for more than the lots
  hold leaves every one of them untouched.
- `ADJUSTMENT` may not name a lot and leaves the lots alone: it sets an absolute quantity, and which
  lots that figure belongs to is a question only a stocktake can answer.

A lot that belongs to a different product, or is held somewhere other than where the stock moved, is
rejected with `400 Bad Request`. A lot still holding stock cannot be deleted — that stock exists and
has to leave through a movement.

Batch tracking is opt-in per product simply by having lots: a product with none behaves exactly as it
did before, and the movement history records the lot on every movement that named one.

### Stock Reservations

A reservation holds stock against an outbound commitment — a sales order, a works order, a customer
collection — so it stops counting as available. Nothing moves: the units stay on the shelf and the
movement history is untouched until the reservation is fulfilled.

| Endpoint | Purpose |
| :--- | :--- |
| `POST /api/products/{id}/reservations` | Hold stock against a commitment (ADMIN) |
| `GET /api/products/{id}/reservations` · `?status=` | A product's reservations, newest first |
| `GET /api/products/{id}/availability?warehouseId=` | On hand, reserved and available |
| `GET /api/reservations/{id}` | A single reservation |
| `POST /api/reservations/{id}/release` | Give the held stock back; nothing moves (ADMIN) |
| `POST /api/reservations/{id}/fulfil` | Ship it: records the `OUT` movement and closes the hold (ADMIN) |

```json
POST /api/products/1/reservations
{
  "reference": "SO-1042",
  "quantity": 12,
  "warehouseId": 1,
  "expiresAt": "2026-08-09T17:00:00Z"
}
```

Available is on hand minus what is already held, floored at zero; reserving more than that is
rejected with `409 Conflict`. A reservation naming a warehouse is checked against, and counts
against, that location's level; one naming none is checked against the product total, so a hold taken
without a location never suppresses a location's availability.

An `expiresAt` makes the hold lapse: a quote nobody took up should not tie up the shelf forever. A
lapsed hold stops counting against availability the moment it expires — no sweep rewrites the row —
is reported with `"expired": true`, and can no longer be fulfilled, though it can still be released
to settle the record.

Fulfilling records the `OUT` movement for the reserved quantity against the reserved location, so the
stock leaves through the ordinary trail: per-warehouse levels, earliest-expiry-first batch
allocation, low-stock notifications and automatic reordering all see it as they see any other
movement. If the stock is no longer there to ship, the movement is refused with `409 Conflict` and
the reservation stays held. Releasing or fulfilling a reservation that is already settled answers
`409 Conflict`.

Reservations are a claim on stock, not a lock on it: an ordinary `OUT` movement still ships whatever
is physically there. When that leaves more reserved than is on hand, availability reads zero rather
than a negative number.

### Purchase Orders & Goods Receipts

An order is raised as a `DRAFT`, sent to the supplier by placing it, and closed by receiving the
goods. Deliveries rarely arrive in one piece, so a receipt says what actually turned up.

| Endpoint | Purpose |
| :--- | :--- |
| `POST /api/purchase-orders` | Raise a `DRAFT` order with its line items, and where it is to be delivered |
| `POST /api/purchase-orders/{id}/place` | Send it to the supplier: `DRAFT` → `PLACED` |
| `POST /api/purchase-orders/{id}/receipts` | Book one delivery, line by line |
| `POST /api/purchase-orders/{id}/receive` · `?warehouseId=` | Receive everything still outstanding at once |
| `POST /api/purchase-orders/{id}/cancel` | Abandon whatever is still outstanding |

An order may name the warehouse it is to be delivered to, which is what the buyer already knows when
the order goes out:

```json
POST /api/purchase-orders
{
  "supplierId": 7,
  "warehouseId": 1,
  "items": [{ "productId": 3, "quantity": 40, "unitPrice": 2.50 }]
}
```

Every receipt against that order books into warehouse 1 without repeating it, and the order reports
it back as `warehouseId` and `warehouseCode`. A `warehouseId` no site carries answers `404 Not
Found` here, when the order is raised, rather than weeks later when the goods turn up.

An order that names no warehouse is delivered where that supplier's goods normally go — the
`defaultWarehouse` recorded on the supplier itself, which is a fact about the trading relationship
rather than a decision taken order by order:

```json
PUT /api/suppliers/7
{
  "name": "Acme Supplies",
  "email": "sales@acme.test",
  "defaultWarehouse": { "id": 1 }
}
```

The supplier reports it back as `defaultWarehouseId` and `defaultWarehouseCode`, and a warehouse no
site carries answers `404 Not Found` when the supplier is saved rather than when an order against it
is raised. The order reads it once, as it is raised, and records the warehouse it was given: moving
a supplier's usual destination later leaves orders already out with it going where they were going.
Naming a `warehouseId` on the order still wins, and remains the way to send one order somewhere
else. A supplier with no default behaves as before: an order that names no warehouse names none, and
a receipt against it books against the product total only.

A receipt names only the lines that arrived, and says where they landed:

```json
POST /api/purchase-orders/4/receipts
{
  "warehouseId": 1,
  "lines": [
    { "itemId": 11, "quantity": 20, "lotCode": "A-2291", "expiryDate": "2026-12-31" },
    { "itemId": 12, "quantity": 5, "warehouseId": 2 }
  ]
}
```

Each booked quantity records an `IN` stock movement at the line's `unitPrice`, exactly as a full
receipt does, so a partial delivery rolls the weighted average cost the same way. Lines left out of
the request are not received and stay outstanding.

The goods are put away where the receipt says they went. The receipt's `warehouseId` applies to every
line that does not name its own, the order's applies when neither says, and the movement lands on
that location's stock level like any other. A `lotCode` books the goods into that lot of the product, starting to track it — held in the
receiving warehouse, expiring on the stated `expiryDate` — when the product does not carry the code
yet, so purchased stock and the lot it arrived as are one record rather than a lot declared by hand
and filled with an invented second movement. A code the product already carries is reused, and
stating it under a different expiry date is rejected with `409 Conflict`. An `expiryDate` without a
`lotCode` answers `400 Bad Request`: an expiry date is a property of a lot, and there is no lot to
hang it on.

A line may be listed more than once, which is how a mixed pallet is expressed — part of it into one
lot, part into another, part onto a second site. Parts agreeing on both the location and the lot are
booked together. What the line may receive in total is still what it has outstanding, counted across
every part it was split into.

`POST /{id}/receive` takes an optional `warehouseId` and books everything outstanding into it,
falling back to the warehouse the order is to be delivered to. It names no lot: a shorthand that
receives whatever is left cannot know which lot each line arrived as. An order naming no warehouse,
received without one, keeps the earliest behaviour exactly — the receipt books against the product
total, with no warehouse and no lot.

Every line reports `receivedQuantity` and `outstandingQuantity`, so purchasing can see what to chase
without recomputing it. The order lands in `PARTIALLY_RECEIVED` while anything is still to come and
reaches `RECEIVED` only when every line is complete — both as a consequence of the receipt, not as a
separate call. Further receipts are accepted against a `PLACED` or `PARTIALLY_RECEIVED` order.

A delivery is one transaction: a line receiving more than it has outstanding answers `409 Conflict`
and nothing at all is booked, and a line that is not on the order answers `404 Not Found`. There is
no over-receipt tolerance — a genuine overage is booked as an ordinary stock movement.

Cancelling is allowed from `PARTIALLY_RECEIVED` and means the supplier will not send the rest: the
stock already received stays, only the outstanding quantity is abandoned. A `RECEIVED` order has
nothing left to abandon and cannot be cancelled.

### Inventory Costing

A product's `price` is what it sells for. What its stock cost is a second figure, `averageCost`: the
weighted average of what the units on hand were acquired at. It is never set from a request — a
create payload naming one is ignored — because a cost is the consequence of a receipt, not a field to
edit.

| Endpoint | Purpose |
| :--- | :--- |
| `POST /api/products/{id}/movements` with `unitCost` | Receive stock at a stated cost |
| `GET /api/reports/valuation` | Every product's quantity, average cost and value, with the total |
| `GET /api/reports/cogs?from=&to=&productId=` | What the stock that left over a window cost |

```json
POST /api/products/1/movements
{
  "type": "IN",
  "quantity": 100,
  "unitCost": 7.00
}
```

Receiving at a stated cost rolls the average: `(onHand × average + received × unitCost) / (onHand +
received)`, kept to four decimal places. Two hundred bought at 4.00 and a hundred at 7.00 leave an
average of 5.0000. A receipt without a `unitCost` is taken in at the running average and leaves it
alone, and the first receipt of a product takes its cost outright.

Every movement records what it was valued at, `unitCost` and `totalCost`, so the history says what
each one was worth and not only how many units it shifted. An outward movement is valued at the
average of the moment — that figure is the cost of goods sold, fixed when the stock leaves and
undisturbed by whatever is received afterwards. An `ADJUSTMENT` ignores a stated cost: a recount
settles how many units are on the shelf, not what they cost.

Receiving a purchase order takes each line in at its `unitPrice`, whether the goods arrive all at
once or over several deliveries, so ordinary purchasing keeps the average honest without anyone
entering the figure twice.

`GET /api/reports/cogs` defaults to the whole record; `from` is inclusive, `to` exclusive, and a
window that ends before it starts is rejected with `400 Bad Request`. Movements recorded before the
system costed stock carry no cost and are summed as units with no value against them, so a history
predating costing reports fewer money than goods.

`GET /api/reports/stock-value` and the CSV, Excel and PDF exports are unchanged: they value stock at
the selling price. A product never received at a stated cost carries an average of zero and is
reported as worth nothing at cost, which is the honest answer — what its stock cost is not known.

### Barcode & QR Codes

Each product carries an optional `barcode` field (EAN/UPC/Code 128 symbol content, unique across
products). A scanner client posts the scanned value to the lookup endpoint to resolve it to a
product, and label images can be rendered on demand.

| Endpoint | Purpose |
| :--- | :--- |
| `GET /api/products/barcode/{barcode}` | Resolves a scanned symbol to a product; `404` when unknown |
| `GET /api/products/{id}/barcode.png` | Code 128 label as `image/png` |
| `GET /api/products/{id}/qrcode.png` | QR code as `image/png` |

Label endpoints encode the product's `barcode`, falling back to its `sku` when no barcode is
assigned, so every product can be labelled and scanned.

### Multi-Tenancy

A single deployment can serve several organisations. Every tenant-owned table carries a `tenant_id`
column holding the tenant's slug, and Hibernate's discriminator-based multi-tenancy stamps that
value on insert and appends it to every query — isolation is enforced in the persistence layer, not
by each service remembering to filter.

The tenant for a request is the tenant of the authenticated account: the login principal carries it,
a filter binds it to the request thread, and it is unbound again when the request completes. Work
running outside a request (startup, scheduled jobs) falls back to the default tenant.

| Endpoint | Purpose |
| :--- | :--- |
| `POST /api/tenants` | Register a tenant (ADMIN) |
| `GET /api/tenants` · `GET /api/tenants/{id}` | Read the tenant registry (ADMIN) |
| `PUT /api/tenants/{id}` | Rename or deactivate a tenant; the slug is immutable (ADMIN) |
| `GET /api/tenants/current` | The tenant the calling account is scoped to |

```json
POST /api/auth/register
{
  "email": "buyer@acme.example",
  "password": "password123",
  "tenantSlug": "acme"
}
```

Registrations naming no `tenantSlug` join the default tenant, and registering into a deactivated
tenant is rejected with `400 Bad Request`. Business keys are unique *within* a tenant rather than
across the installation — two tenants may both hold SKU `SKU-1` or a category named `Tools` — while
login stays global, since an account's email identifies both the user and the tenant it belongs to.
A resource belonging to another tenant answers `404 Not Found`, exactly as a non-existent one does.

| Property | Environment variable | Default | Purpose |
| :--- | :--- | :--- | :--- |
| `multitenancy.default-tenant` | `DEFAULT_TENANT` | `default` | Tenant used outside requests and for registrations naming none |

The tenant registry spans the whole installation, so managing it is an ADMIN-only operation; every
other endpoint only ever sees the caller's own tenant.

### Paged Listings

Every listing that can grow without bound returns one page at a time, through the same `page` /
`size` / `sort` parameters and the same envelope. Page size defaults to 20 and is capped at 100, so
no single call can pull a whole table:

| Endpoint | Default order | Sortable fields |
| :--- | :--- | :--- |
| `GET /api/products` | `id,asc` | `id`, `name`, `sku`, `price`, `quantity`, `reorderThreshold`, `createdAt`, `updatedAt` |
| `GET /api/products/{id}/movements` | `createdAt,desc` | `id`, `createdAt`, `quantity`, `type` |
| `GET /api/stock-transfers` | `createdAt,desc` | `id`, `createdAt`, `quantity` |
| `GET /api/stock-counts` | `createdAt,desc` | `id`, `createdAt`, `completedAt`, `status` |
| `GET /api/purchase-orders` | `createdAt,desc` | `id`, `createdAt`, `updatedAt`, `status` |
| `GET /api/audit-logs` | `createdAt,desc` | `id`, `createdAt`, `entityType`, `entityId`, `action`, `username` |

The filters each listing already had (`productId`, `warehouseId`, `status`, `supplierId`) still
apply, and combine with paging. A `sort` field outside the listing's own allowlist, a negative
`page`, or a `size` outside `1..100` is a client error and answers `400 Bad Request` — an unknown
field never reaches the database.

The reference-table listings — `/api/categories`, `/api/suppliers`, `/api/warehouses`,
`/api/tenants` — are deliberately unpaged: they are bounded by how many categories or sites an
organisation has.

### Product Listing: Filtering

`GET /api/products` also narrows the catalogue by content:

| Parameter | Default | Purpose |
| :--- | :--- | :--- |
| `search` | — | Case-insensitive text matched against the product name *and* SKU |
| `categoryId` | — | Keeps only products in that category |
| `supplierId` | — | Keeps only products from that supplier |
| `minPrice` / `maxPrice` | — | Keeps only products inside the price band |
| `lowStock` | `false` | Keeps only products at or below their reorder threshold |

Filters are optional and combine with AND, so `?search=widget&categoryId=3&lowStock=true&sort=price,desc`
is a single query. A page comes back inside an envelope carrying the metadata needed to walk the rest:

```json
GET /api/products?page=0&size=2&sort=price,desc
{
  "content": [ { "id": 7, "name": "Widget Pro", "price": 49.99, "…": "…" } ],
  "page": 0,
  "size": 2,
  "totalElements": 137,
  "totalPages": 69,
  "first": true,
  "last": false
}
```

The category and supplier are fetched with the page, so a product listing costs one query regardless
of how many rows it carries. `GET /api/products/low-stock` is unchanged and still returns a plain
array; it is the same result as `?lowStock=true`, without paging.

### Response Shape

Endpoints return response DTOs, never JPA entities. Related records are flattened to their
identifier and a label rather than nested whole, and persistence details such as the `tenant_id`
discriminator stay out of the payload:

```json
GET /api/products/1
{
  "id": 1,
  "name": "Widget",
  "sku": "SKU-1",
  "price": 19.99,
  "quantity": 42,
  "reorderThreshold": 10,
  "categoryId": 3,
  "categoryName": "Tools",
  "supplierId": 2,
  "supplierName": "Acme Supplies"
}
```

The application runs with `spring.jpa.open-in-view=false`, so the persistence session is already
closed by the time a response is written. Mapping to DTOs — and fetching the associations those
DTOs read — is what keeps serialisation off uninitialised proxies.

A product is attached to a category or supplier by naming its id; the named record must exist, or
the request is rejected with `404 Not Found`:

```json
POST /api/products
{
  "name": "Widget",
  "sku": "SKU-1",
  "price": 19.99,
  "quantity": 42,
  "category": { "id": 3 },
  "supplier": { "id": 2 }
}
```

### API Documentation (Swagger / OpenAPI)

Once the application is running, the REST API is documented interactively:

| Resource | URL |
| :--- | :--- |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs |

Both are publicly accessible (no token required). To call protected endpoints from Swagger UI:

1. Register or log in via the **Auth** endpoints (`POST /api/auth/register` or `POST /api/auth/login`) and copy the returned `token`.
2. Click the **Authorize** button, paste the token (without the `Bearer ` prefix) and confirm.
3. Swagger UI now sends `Authorization: Bearer <token>` on every request. Write operations still require the `ADMIN` role.
