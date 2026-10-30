# AGENTS.md --- HoLa Backend

This file defines the working instructions for AI coding agents and
contributors in this repository.

## 1. Read before making changes

Before implementing a task:

1.  Read `ARCHITECTURE.md` and follow its architecture, module-boundary,
    API, persistence, security, and testing rules.
2.  Inspect the existing source code, tests, `pom.xml`, application
    configuration, Flyway migrations, and relevant documentation.
3.  Identify the owning business module and understand its existing
    conventions before editing.
4.  If an applicable `SKILL.md` exists in the repository, read and
    follow it. Do not assume a skill exists if it is not present.

Do not infer that a proposed module or capability in `ARCHITECTURE.md`
has already been implemented. Verify the actual codebase.

## 2. General implementation principles

-   Preserve the existing technology stack and established conventions
    unless the task explicitly requires a change.
-   Prefer small, focused, production-oriented changes over broad
    refactoring.
-   Do not introduce microservices, new infrastructure, libraries,
    abstractions, or design patterns without a concrete requirement.
-   Keep business logic out of controllers, entities, mappers, and
    configuration classes.
-   Use constructor injection; do not use field injection.
-   Use clear names and single-responsibility classes.
-   Avoid duplicated business rules and unnecessary abstractions.
-   Do not leave placeholder implementations, empty methods, or silently
    ignored exceptions.
-   Do not claim a task is complete unless the relevant implementation
    and verification have actually been performed.

## 3. Module boundaries

-   Organize business code under `org.anest.hola.modules.<module>`.
-   Put code in the module that owns the business capability and data.
-   A module must not access another module's repositories or modify
    another module's entities directly.
-   Cross-module communication must use an intentional public service,
    facade, or contract exposed by the owning module.
-   Do not create circular dependencies between modules.
-   Keep `common` limited to genuinely shared cross-cutting components.
    Do not use it as a dumping ground for business code.
-   Do not create a module or package solely to satisfy a proposed
    directory tree; create it when a real responsibility exists.

## 4. API and application-layer rules

-   Follow the existing `/api/v1` API convention.
-   Use request and response DTOs at API boundaries; do not expose JPA
    entities directly.
-   Apply Jakarta Bean Validation to untrusted request input.
-   Enforce authentication and authorization on the backend. Frontend
    visibility is not an authorization control.
-   Keep controllers focused on HTTP concerns and delegate use cases to
    services.
-   Use consistent response and error conventions from the existing
    `common` components.
-   Do not expose stack traces, secrets, password hashes, OTP
    values/hashes, or token hashes in API responses or logs.
-   Keep HTTP status codes and application error codes meaningful and
    consistent.

## 5. Persistence and database changes

-   Flyway is the source of truth for database schema changes.
-   Every schema change must be delivered as a new Flyway migration.
-   Never edit a migration that has already been applied in a shared or
    production environment; add a new migration instead.
-   Keep Hibernate schema management in validation mode
    (`ddl-auto=validate`) for normal application environments. Do not
    use Hibernate `update`, `create`, or `create-drop` as a substitute
    for migrations.
-   Update entity mappings, repositories, services, and tests
    consistently with each schema change.
-   Consider foreign keys, unique constraints, indexes, nullability,
    defaults, data migration, and backward compatibility.
-   Do not assume H2 behavior is equivalent to MySQL. Use
    MySQL-compatible integration tests for behavior that depends on the
    production database, Flyway, SQL dialect, or database constraints.
-   Avoid returning entities from repositories across module boundaries.

## 6. Security and sensitive data

-   Follow deny-by-default authorization principles.
-   Add public endpoints only when explicitly required by the use case.
-   Validate ownership and permissions for resource access; do not rely
    only on role checks when object-level authorization is needed.
-   Never commit `.env` files, credentials, private keys, access tokens,
    refresh tokens, OTPs, or other secrets.
-   Update `.env.example` only with non-sensitive variable names and
    safe example values.
-   Do not log passwords, raw tokens, OTPs, authorization headers, or
    sensitive personal data.
-   Apply appropriate abuse protection to authentication and OTP-related
    endpoints.
-   Configure CORS narrowly for the intended environments.
-   Avoid account-enumeration leaks in authentication and password-reset
    flows.

## 7. Transactions and external side effects

-   Define transaction boundaries at service/use-case level.
-   Use `@Transactional` for operations that must be atomic; use
    `readOnly = true` for read-only operations where appropriate.
-   Do not hold database transactions open while waiting for slow
    external calls such as SMTP or remote HTTP services.
-   Consider consistency and failure handling when a database operation
    is combined with an external side effect.
-   Do not swallow exceptions or report success when a required
    operation failed.

## 8. Testing requirements

Choose tests based on the risk and behavior being changed:

-   Unit tests for business rules and isolated service behavior.
-   MVC/API tests for request validation, response contracts, and
    security behavior.
-   Integration tests for Spring component interaction.
-   MySQL/Testcontainers tests for migrations, mappings, constraints,
    and MySQL-specific behavior when relevant.
-   Architecture tests when changes affect module boundaries or
    dependency rules.

For behavior changes, add or update tests for relevant success cases,
failure cases, validation, authorization, and edge cases. Do not modify
or weaken tests merely to make a failing implementation pass.

## 9. Configuration, dependencies, and documentation

-   Keep environment-specific configuration in the appropriate Spring
    profile.
-   Read secrets from environment variables or an appropriate
    secret-management mechanism.
-   Do not expose production-only operational details in public
    endpoints or logs.
-   Add dependencies only when existing capabilities cannot reasonably
    meet the requirement.
-   Update OpenAPI descriptions and relevant documentation when API
    behavior changes.
-   Update `PROJECT_STRUCTURE.md` when the repository structure
    materially changes.
-   Update `ARCHITECTURE.md` when an intentional architecture decision
    or module boundary changes.

## 10. Required workflow

For each task:

1.  **Understand** --- restate the requested outcome and inspect
    relevant code.
2.  **Plan** --- identify affected modules, contracts, database changes,
    security implications, and tests.
3.  **Implement** --- make the smallest complete change that satisfies
    the requirement.
4.  **Verify** --- run relevant tests and build checks; inspect the
    final diff.
5.  **Report** --- summarize changes, migrations, tests executed,
    results, and any remaining risks or follow-up work.

If a command cannot be run, state that clearly. Never invent test
results or claim unverified behavior.

## 11. Change review checklist

Before finishing, verify:

-   [ ] The change belongs to the correct module.
-   [ ] No module boundary or dependency rule is violated.
-   [ ] API input is validated and output does not expose entities or
    sensitive data.
-   [ ] Authentication, authorization, and ownership checks are correct.
-   [ ] Database changes use a new Flyway migration.
-   [ ] Transaction boundaries and external side effects are handled
    safely.
-   [ ] Relevant tests have been added or updated.
-   [ ] Configuration contains no committed secrets.
-   [ ] Documentation is updated where necessary.
-   [ ] The final report accurately describes what was changed and what
    was verified.

## 12. Conflict resolution

When instructions or conventions appear to conflict:

1.  Follow explicit task requirements, provided they do not violate
    security or data-integrity requirements.
2.  Follow this file and `ARCHITECTURE.md`.
3.  Preserve established repository conventions where they do not
    conflict with the above.
4.  Prefer the simplest safe solution and explain material trade-offs.

If a requirement is ambiguous and materially affects data integrity,
security, public API compatibility, or architecture, ask for
clarification before making an irreversible decision.
