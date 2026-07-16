# Pull Request

## Summary

This Pull Request merges the final Release v1.0 configurations into `main`. It completes the integration of Docker Compose orchestration (Issue #34), GitHub Actions CI pipeline automation (Issue #35), and the stable `1.0.0` version release preparation (Issue #36).

---

## Related Issue

Closes #34, Closes #35, Closes #36

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

- **Docker Compose Setup (Issue #34)**: Created `docker-compose.yml` to orchestrate the Spring Boot application and a PostgreSQL database container with database health checks (`pg_isready`) and volume persistence.
- **GitHub Actions CI Pipeline (Issue #35)**: Configured `.github/workflows/ci.yml` to automatically spin up a database container, compile the app, run Checkstyle rules, and execute tests verifying JaCoCo code coverage gates on every push/PR.
- **Release v1.0 Configuration (Issue #36)**: Bumped the project packaging version in `pom.xml` to `1.0.0`, checked off completed roadmap tasks in `ROADMAP.md`, updated milestones M2-M5 to Done, and updated the progress tracker in `PROGRESS.md`.

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
- Ran local Maven verify compilation.
- Validated docker-compose and workflow YAML schemas.
- Confirmed project version bump to 1.0.0 in maven configuration.
```

---

## Additional Notes

This pull request completes Phase 5 of the development roadmap, stabilizing the baseline application stack.
