---
name: rest-api
description: Use when designing or changing REST endpoints, request/response DTOs, validation, status codes, pagination, filtering, error formats or OpenAPI documentation.
---

# REST API design

## Project contract (verified — see `hola-architecture/references/project-conventions.md`)
- Routes: `/api/v1/<plural-kebab-resource>` owned by one module's controller (`modules/<module>/controller`). Use `/me` for current-user shortcuts (`/api/v1/users/me`), `/api/v1/<parents>/{parentId}/<children>` for max 1 level of nesting, and `POST /api/v1/<resources>/{id}/<action>` for non-CRUD state transitions. JSON fields use `camelCase`.
- Every endpoint returns `ApiResponse<T>` via `ApiResponse.success(data)` or `ApiResponse.success(MessageUtils.get(MessageKeys.X), data)` (`code = 1000`).
- Lists return `ApiResponse<PageResponse<T>>` built in the service with `PageResponse.from(page.map(mapper::toResponse))`.
- Errors: throw `AppException(ErrorCode.X)` in services; never build error responses in controllers. Inspect `GlobalExceptionHandler` for framework exceptions already mapped (`@Valid` body, `@PathVariable`/`@RequestParam` constraints, type mismatch, missing param, malformed JSON, `DataIntegrityViolationException` → 409 `DATA_CONFLICT`).
- Validation failures (`@Valid` / parameter constraints) → HTTP 400, `code 9002`, `data` = `{field: message}` (first error per field via `putIfAbsent`).
- Internationalization (i18n): user-facing error/success/validation messages live in `src/main/resources/messages.properties` (Vietnamese), referenced via `MessageKeys` + `MessageUtils.get(...)` or `{validation.*}` in Bean Validation annotations. Never hardcode user-facing strings in DTOs, controllers or `ErrorCode`.
- Request/response DTOs live in `modules/<module>/dto/`, named `<Action><Resource>Request` (`RegisterRequest`, `LoginRequest`) and `<Resource>Response` (`UserResponse`, `AuthResponse`). Map with the module's MapStruct mapper.
- OpenAPI: `@Tag` on the controller, `@Operation(summary = ...)` on each endpoint. **Name collision warning:** `io.swagger.v3.oas.annotations.responses.ApiResponse` collides with `org.anest.hola.common.dto.ApiResponse` — always import our DTO and use the fully-qualified name if Swagger `@ApiResponse` annotations are ever needed. Bearer auth is global; public endpoints must also be listed in `SecurityConfig.PUBLIC_ENDPOINTS` (see `spring-security`).
- Current user ID: `Long.valueOf(authentication.getName())`. Role checks: `@PreAuthorize("hasRole('...')")`.
- Template: `hola-architecture/references/code-examples.md`.

## DTOs and validation
- Never return JPA entities from controllers or embed them in DTOs.
- Validate input at the boundary with Bean Validation (`@NotBlank`, `@Size` matching column length, `@Email`, `@Positive`, ...) using `{validation.*}` message keys, plus explicit business validation in the service.
- Do not rely on database errors as the primary validation mechanism: check uniqueness first (`existsBy...` → `throw new AppException(ErrorCode.X_ALREADY_EXISTS)`), keeping the DB unique constraint + `DataIntegrityViolationException` handler (`9005`) as the race-condition backstop.
- Avoid mass assignment: request DTOs contain only caller-settable fields. Never bind `id`, `roles`, `status`, `ownerId`/`userId` or audit fields from the request body; take the actor/owner ID from `Authentication`.
- Normalize string inputs (`trim()`, lowercase emails with `Locale.ROOT`) in the service before lookup/persistence.

## HTTP semantics
- `GET` → 200 OK.
- `POST` (create) → 201 Created (`ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(...))` or `@ResponseStatus(HttpStatus.CREATED)`). `Location` header is not required unless specified.
- `POST` (action, e.g. `/login`, `/refresh`, `/logout`) → 200 OK.
- `PUT` (full replacement) / `PATCH` (partial update) → 200 OK returning the updated `<Resource>Response`. For `PATCH`, use nullable wrapper fields on `Update<Resource>Request` and `nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE` on the MapStruct update method (`void updateEntity(UpdateThingRequest req, @MappingTarget Thing entity)`); if a field must be explicitly clearable vs omitted, confirm the contract first.
- `DELETE` → **200 OK** returning `ApiResponse<Void>` (`ApiResponse.success(MessageUtils.get(key), null)`). Do not use 204 No Content.

## Pagination, sorting and filtering
- Accept `Pageable` in controller methods (`@ParameterObject Pageable pageable` for Swagger UI). Spring Data Web is configured in `application.properties` (`default-page-size=20`, `max-page-size=100`, `one-indexed-parameters=false`).
- Always enforce a deterministic default sort (e.g. `@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)`) and **whitelist allowed sort properties** in the service before querying — never pass arbitrary client sort fields to JPA, as unknown/unindexed properties cause 500 `PropertyReferenceException` or slow queries.
- Map entities to DTOs inside the service transaction: `PageResponse.from(repo.findAll(pageable).map(mapper::toResponse))`.

## Compatibility and testing
- Treat API shape as a client contract (web and mobile). Prefer additive changes; do not rename/remove fields or change `ErrorCode` numbers silently.
- Date/time fields in DTOs use `LocalDateTime` (serialized as ISO-8601); `ApiResponse.timestamp` uses `Instant`.
- Keep authorization server-side and scoped to the target resource (ownership checks in the service, not only role checks).
- Every new/changed endpoint requires `@WebMvcTest` coverage for success status/body, validation failure (`9002`), unauthenticated (`1001`), forbidden (`1002`) and domain errors (see `testing`).

## Deliverable checklist
When proposing or reviewing an endpoint, fill out `references/endpoint-template.md`.
