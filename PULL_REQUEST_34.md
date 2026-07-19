# Pull Request

## Summary

This Pull Request introduces the Docker Compose configuration to orchestrate the Smart Inventory Management System and its PostgreSQL database dependency. It sets up the service configurations, environment variables, health checks, and data persistence layers.

---

## Related Issue

Closes #34

---

## Type of Change

- [ ] Feature
- [ ] Bug Fix
- [ ] Refactoring
- [ ] Documentation
- [ ] Performance Improvement
- [ ] Test
- [ ] CI/CD
- [x] Build

---

## What Changed

- Created a `docker-compose.yml` file configuring two services: `db` (using `postgres:15-alpine`) and `app` (which builds from the local `Dockerfile`).
- Implemented a health check on the PostgreSQL container using `pg_isready` to ensure it is healthy before the app container attempts to connect.
- Injected database credentials, connection URL properties, and a safe development JWT secret key to the Spring Boot container via environment variables.
- Configured a named volume `postgres_data` to ensure database storage is persisted across container lifecycle events.
- Updated `PROGRESS.md` to reflect the completion of Issue #34.

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

Describe how this change was tested.

```
- Validated the schema and configuration parameters in docker-compose.yml.
- Cross-checked service environment variables with application.properties.
```

---

## Additional Notes

Developers can spin up the complete local system by running:
```bash
docker compose up --build
```
This builds the Spring Boot container and starts the database automatically.
