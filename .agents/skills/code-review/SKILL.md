---
name: code-review
description: Use when reviewing a diff or proposed change for correctness, maintainability, security, data integrity, compatibility, performance and test quality.
---

# Code review

## Review method
1. Inspect the task, diff, surrounding implementation, tests, build configuration and relevant skills.
2. Prioritize actionable defects over stylistic preferences. Confirm each finding against actual code and project behavior.
3. Report findings with severity, file/line, scenario, impact and a concrete remedy. Avoid speculative findings without evidence.
4. Distinguish verified defects from questions or risks that need confirmation.

## Review priorities
- Correctness: null/empty/boundary handling, state transitions, race conditions and exception behavior.
- Security: missing authorization, IDOR, insecure defaults, injection, mass assignment, sensitive logging, secret leakage and unsafe deserialization.
- Data integrity: constraints, `@Transactional` boundaries (e.g., self-invocation bypass, slow I/O inside transactions, missing rollback rules), concurrent updates/locking, migration safety and accidental deletion.
- API compatibility: status codes, DTO fields, validation, pagination and changed contracts.
- Persistence performance: N+1 queries, unbounded reads, missing indexes for demonstrated queries, unnecessary eager loading.
- Maintainability: module boundary violations, duplicate business logic, unclear ownership and unjustified dependencies.
- Tests: missing negative/regression tests, flaky tests and tests that assert implementation rather than behavior.

## HoLa-specific checks
Compare against `hola-architecture/references/project-conventions.md` (if available in context). In particular:
- Business rules remain in their owning module (`modules/<module>`); no import of another module's repository or direct entity mutation across modules.
- Controllers return `ApiResponse<T>` / `PageResponse<T>` (via `Pageable` + sort whitelist); `DELETE` returns 200 OK with `ApiResponse<Void>`; errors go through `AppException(ErrorCode.X)` — no ad-hoc error bodies, no new envelope. User-facing messages use `messages.properties` (`MessageKeys` / `{validation.*}`), never hardcoded strings.
- Entities extend `BaseEntity`, no `@Data`/generated `equals`/`hashCode`/`toString`, `EnumType.STRING`, explicit `@Column` matching the migration.
- Every entity change has a matching new Flyway migration (`V<N+1>__...sql`); existing migrations are never edited.
- DDL follows naming (`uk_/fk_/idx_`), `utf8mb4_unicode_ci`, `DATETIME(6)` audit columns.
- Every data-modifying (write) method in a service must be annotated with `@Transactional`.
- No lazy loading outside transactions (`open-in-view=false`); list endpoints are paginated.
- New non-public endpoints are not added to `PUBLIC_ENDPOINTS`; role checks via `@PreAuthorize`; ownership checked in the service; owner/user ID taken from the principal, not the request body.
- New properties are added to `application.properties`, `application-prod.properties` (no secret defaults) and `.env.example`.
- Shared code is genuinely reusable and does not become a dumping ground for domain logic.
- Unapproved product requirements are not introduced.

## Reporting format
List findings from highest to lowest severity (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`). 
For each finding:
- `[Severity] path:line — issue; impact; recommended fix.`

Then list important open questions and test/verification gaps. If no findings are identified, state clearly what was reviewed and what could not be verified; do not imply proof of absence of defects.

Do not edit code during a review-only task unless implementation is explicitly requested.