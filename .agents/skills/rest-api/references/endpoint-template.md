# REST endpoint design template

- **Use case & owning module:**
- **Method and path:** `/api/v1/...`
- **Authentication / authorization:** (`PUBLIC_ENDPOINTS` / authenticated / `@PreAuthorize` / service ownership check)
- **Request DTO & `{validation.*}` rules:**
- **Response DTO:** (`ApiResponse<T>` or `ApiResponse<PageResponse<T>>`)
- **Success HTTP status & message key:** (200 OK / 201 Created; `DELETE` = 200 OK `ApiResponse<Void>`)
- **Domain `ErrorCode`s & HTTP statuses:**
- **Pagination / allowed sort fields / filters:** (`Pageable`, default sort, whitelist)
- **Service transaction & entity graph:**
- **Tests (`@WebMvcTest` + service unit tests):**
