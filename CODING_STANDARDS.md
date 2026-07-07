# Coding Standards — Smart Inventory Management System

This document defines the coding standards, naming conventions, and formatting rules for this project.  
All rules are automatically enforced at build time via **Checkstyle** (runs during `mvn validate`).

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Package Structure](#package-structure)
3. [Naming Conventions](#naming-conventions)
4. [Code Formatting](#code-formatting)
5. [Lombok Usage](#lombok-usage)
6. [Layer Responsibilities](#layer-responsibilities)
7. [Exception Handling](#exception-handling)
8. [REST API Design](#rest-api-design)
9. [Testing Standards](#testing-standards)
10. [Git Workflow](#git-workflow)

---

## Tech Stack

| Technology | Version | Role |
|---|---|---|
| Java | 17 | Primary language |
| Spring Boot | 4.1.x | Application framework |
| Spring Data JPA | — | Database access layer |
| Spring Security | — | Authentication & authorisation |
| Spring Validation | — | Input validation |
| Flyway | — | Database migrations |
| PostgreSQL | — | Relational database |
| Lombok | — | Boilerplate reduction |

---

## Package Structure

```
com.example.smartinventory
├── config/          # Spring @Configuration classes (Security, Beans, etc.)
├── controller/      # @RestController — HTTP request handling only
├── service/         # @Service — business logic
├── repository/      # @Repository — Spring Data JPA interfaces
├── model/
│   ├── entity/      # @Entity classes mapped to DB tables
│   └── dto/         # Data Transfer Objects (request/response bodies)
├── exception/       # Custom exceptions & @RestControllerAdvice handler
└── util/            # Stateless utility/helper classes
```

> **Rule**: Each class must live in exactly one of these packages. Cross-cutting concerns
> (e.g., audit fields) belong in `model/entity` or `util/`.

---

## Naming Conventions

### Classes

| Kind | Convention | Example |
|---|---|---|
| All classes / interfaces / enums / records | `UpperCamelCase` | `ProductService` |
| Entity | `<Domain>` | `Product`, `Warehouse` |
| DTO (request) | `<Domain>Request` | `CreateProductRequest` |
| DTO (response) | `<Domain>Response` | `ProductResponse` |
| Repository | `<Domain>Repository` | `ProductRepository` |
| Service (interface) | `<Domain>Service` | `ProductService` |
| Service (implementation) | `<Domain>ServiceImpl` | `ProductServiceImpl` |
| Controller | `<Domain>Controller` | `ProductController` |
| Exception | `<Reason>Exception` | `ProductNotFoundException` |
| Configuration | `<Concern>Config` | `SecurityConfig` |

### Methods & Variables

| Kind | Convention | Example |
|---|---|---|
| Methods | `lowerCamelCase` | `findProductById()` |
| Local variables | `lowerCamelCase` | `productList` |
| Method parameters | `lowerCamelCase` | `productId` |
| Boolean methods | `is` / `has` prefix | `isAvailable()`, `hasStock()` |

### Constants & Packages

| Kind | Convention | Example |
|---|---|---|
| Constants (`static final`) | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Packages | all lowercase, no underscores | `com.example.smartinventory.service` |

---

## Code Formatting

All rules below are enforced by [`checkstyle.xml`](./checkstyle.xml) and [`  .editorconfig`](./.editorconfig).

| Rule | Value |
|---|---|
| Encoding | UTF-8 |
| Line endings | LF (`\n`) |
| Indent style | Spaces only — **no tabs** |
| Indent size | **4 spaces** |
| Max line length | **120 characters** |
| Braces | Always required, even for single-line `if`/`for`/`while` |
| Opening brace | Same line (K&R style) |
| Imports | No wildcard imports; static imports first, alphabetically sorted |
| Trailing whitespace | Not allowed |
| Final newline | Required in every file |

### Blank Lines

- **1 blank line** between methods inside a class.
- **1 blank line** between fields and the first method.
- **No blank lines** at the top/bottom of a block body.

---

## Lombok Usage

Prefer the following Lombok annotations to reduce boilerplate:

| Scenario | Preferred annotation |
|---|---|
| Plain entity / DTO with all getters, setters, equals, hashCode | `@Data` |
| Immutable DTO / value object | `@Value` |
| Builder pattern | `@Builder` |
| Constructor injection (Spring beans) | `@RequiredArgsConstructor` |
| Logging | `@Slf4j` |

**Rules:**
- Do **not** use `@Data` on JPA `@Entity` classes — it generates `equals`/`hashCode` based on all fields,
  which breaks Hibernate's entity identity. Use `@Getter @Setter @ToString @EqualsAndHashCode(of = "id")` instead.
- Always use `@RequiredArgsConstructor` for `@Service` and `@Controller` classes (enables final-field injection).

---

## Layer Responsibilities

```
HTTP Request
     │
     ▼
Controller     → Validates input (@Valid), delegates to Service, maps to DTO response
     │
     ▼
Service        → Business logic, transactions (@Transactional), calls Repository/external APIs
     │
     ▼
Repository     → Data access only; no business logic; extends JpaRepository
     │
     ▼
Database
```

**Rules:**
- Controllers must **never** call repositories directly.
- Services must **never** return `@Entity` objects — always map to a DTO.
- Entities must **never** leak to the HTTP layer.

---

## Exception Handling

- Define custom exceptions in `com.example.smartinventory.exception`.
- All exceptions extend `RuntimeException` for unchecked propagation.
- A single `@RestControllerAdvice` class (`GlobalExceptionHandler`) handles all exceptions centrally.

```java
// Good
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product with id " + id + " not found");
    }
}
```

---

## REST API Design

| Verb | Usage | Example |
|---|---|---|
| `GET` | Read resource(s) | `GET /api/v1/products` |
| `POST` | Create resource | `POST /api/v1/products` |
| `PUT` | Full replace | `PUT /api/v1/products/{id}` |
| `PATCH` | Partial update | `PATCH /api/v1/products/{id}` |
| `DELETE` | Remove resource | `DELETE /api/v1/products/{id}` |

**Rules:**
- All endpoints are prefixed `/api/v1/`.
- Resource names are **plural nouns** in **kebab-case** (`/purchase-orders`).
- Use standard HTTP status codes (`200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `500`).
- Return consistent error bodies using a shared `ErrorResponse` DTO.

---

## Testing Standards

| Layer | Test type | Annotation |
|---|---|---|
| Service | Unit test (mocks) | `@ExtendWith(MockitoExtension.class)` |
| Repository | Slice test | `@DataJpaTest` |
| Controller | Slice test | `@WebMvcTest` |
| Full flow | Integration test | `@SpringBootTest` |

**Rules:**
- Test method names follow the pattern: `methodName_givenCondition_expectedResult`.
- Every public service method must have at least one unit test.
- Do **not** use `@Autowired` in tests — use constructor injection or Mockito's `@InjectMocks`.

---

## Git Workflow

### Branch Naming

```
issue-<N>-<short-slug>
```

Examples: `issue-7-configure-code-style`, `issue-12-product-crud`

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short description>

[optional body]

[optional footer: Closes #<issue-number>]
```

| Type | When to use |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `chore` | Config, tooling, build changes |
| `docs` | Documentation only |
| `refactor` | Code change without feature/fix |
| `test` | Adding or updating tests |
| `style` | Formatting/whitespace (no logic change) |

### Pull Request Rules

- Reference the issue in the PR description: `Closes #<N>`.
- Use the PR template (`.github/pull_request_template.md` or `pr_template.md`).
- At least **1 reviewer** approval required before merge.
- All Checkstyle checks must pass in CI before merging.
