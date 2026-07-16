# Pull Request

## Summary

This Pull Request prepares and triggers the Release v1.0 of the Smart Inventory Management System. It updates the project packaging version to stable `1.0.0`, brings development roadmap documentation up to date, and marks all baseline features as completed.

---

## Related Issue

Closes #36

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

- Bumped the project version in `pom.xml` from `0.0.1-SNAPSHOT` to `1.0.0`.
- Updated `ROADMAP.md` by marking Phase 2, Phase 3, Phase 4, and Phase 5 as complete and checking off all implemented tasks.
- Marked milestones M2 (Data Layer Ready), M3 (MVP API), M4 (Secured Release), and M5 (Tested Release) as Done.
- Updated `PROGRESS.md` to reflect the completion of Issue #35 and the active state of Issue #36.

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
- Executed 'mvnw checkstyle:check' to verify style standards.
- Checked maven build output logs to verify that the project builds as version 1.0.0.
```

---

## Additional Notes

This release stabilizes the core system foundation, security models (JWT and role-based controls), Docker orchestration patterns, and CI testing configurations.
