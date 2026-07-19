# Pull Request

## Summary

This Pull Request introduces the GitHub Actions Continuous Integration (CI) configuration to automate compilation, code style compliance, and the execution of the test suite (verifying correct packaging and code coverage ratios) on pushes and pull requests.

---

## Related Issue

Closes #35

---

## Type of Change

- [ ] Feature
- [ ] Bug Fix
- [ ] Refactoring
- [ ] Documentation
- [ ] Performance Improvement
- [ ] Test
- [x] CI/CD
- [ ] Build

---

## What Changed

- Created a GitHub Actions workflow `.github/workflows/ci.yml` that triggers on pushes and pull requests to `main` and active development branches.
- Configured a PostgreSQL service container inside the workflow using `postgres:15-alpine` to enable database connection checks during integration tests.
- Configured JDK 17 (Temurin) setup with Maven dependency caching to speed up builds.
- Integrated execution steps running `./mvnw clean verify` to run Checkstyle auditing, execute tests, and check JaCoCo code coverage limits.
- Updated `PROGRESS.md` to reflect the completion of Issue #35.

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
- Validated YAML syntax in .github/workflows/ci.yml.
- Checked configuration parameter consistency with environment variables.
```

---

## Additional Notes

None.
