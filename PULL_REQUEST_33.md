# Pull Request

## Summary

This Pull Request introduces the necessary configuration to containerize the Smart Inventory Management System application using Docker. It defines a multi-stage build process to optimize image size and includes security best practices such as running the application as a non-privileged user.

---

## Related Issue

Closes #33

---

## Type of Change

- [ ] Feature
- [ ] Bug Fix
- [ ] Refactoring
- [ ] Documentation
- [ ] Performance Improvement
- [ ] Test
- [x] CI/CD
- [x] Build

---

## What Changed

- Created a multi-stage `Dockerfile` with a Maven build stage (`maven:3.9.6-eclipse-temurin-17-alpine`) and a minimal JRE runtime stage (`eclipse-temurin:17-jre-alpine`).
- Configured a non-privileged user and group (`spring:spring`) inside the runtime container to execute the application securely.
- Created a `.dockerignore` file to exclude local build artifacts, wrapper scripts, source repository metadata, and IDE configs from the Docker daemon build context.
- Updated `PROGRESS.md` to reflect the completion of Issue #33.

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
- Ran local build package using 'mvnw clean package -DskipTests' to verify the jar packaging.
- Inspected the Dockerfile configuration and verified correct JRE/JDK versions match Java 17.
- Inspected .dockerignore contents to verify local target/ and configuration files are excluded.
```

---

## Additional Notes

The container exposes port 8080 by default. Active profiles and database configurations can be passed to the container at runtime via environment variables (e.g., `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`).
