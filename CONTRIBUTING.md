# Contributing

Thanks for your interest in improving the Smart Inventory Management System. This guide covers how to get set up and submit changes.

## Prerequisites

- Java 17 (JDK)
- Maven 3.9+
- Docker + Docker Compose (optional, for running PostgreSQL locally)

## Getting Started

1. Fork and clone the repository.
2. Copy `.env.example` to `.env` and adjust values for your environment.
3. Start dependencies (PostgreSQL) with `docker compose up -d db`, or point the app at your own database.
4. Build and run the tests:

   ```bash
   mvn clean verify
   ```

5. Run the application:

   ```bash
   mvn spring-boot:run
   ```

The API docs are then available at `/swagger-ui.html`.

## Branching

- Branch off `main`.
- Use a descriptive prefix: `feature/…`, `fix/…`, `docs/…`, `chore/…`, `test/…`.

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <short summary>
```

Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`.

## Pull Requests

- Fill in the PR template.
- Reference the related issue (e.g. `Closes #12`).
- Ensure `mvn clean verify` passes — build, Checkstyle, tests, and JaCoCo coverage gates must all be green.
- Keep changes focused; one logical change per PR.

## Code Style

- Follow the existing layered architecture (Controller → Service → Repository).
- Checkstyle runs in CI; fix any reported violations before requesting review.
- Add or update tests for any behavioural change.

## Reporting Bugs & Requesting Features

Open an issue using the appropriate template. Include steps to reproduce, expected vs. actual behaviour, and environment details for bugs.
