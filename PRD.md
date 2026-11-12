# Product Requirements Document: Authentication & Cookie Session

## 1. Product overview

Authentication & Cookie Session is a learning project that implements a Ronaldo photo gallery to provide practical experience with Spring Security authentication, cookie-based sessions, and API authorization.

The intended learner already understands the basic flow of a Spring application and wants an initial, hands-on introduction to Spring Security, especially managing authentication and cookie sessions to protect APIs and build a more secure backend.

## 2. Problem and learning objective

The project provides a concrete setting for learning how a backend authenticates an administrator, maintains a server-side session, sends and receives a session cookie, and restricts administrative API operations. Image upload and deletion make the access-control rules observable in a small application.

The application is a learning exercise. It is not represented as a validated production security solution.

## 3. Users and roles

### Visitor

- Can open the website and browse the public gallery.
- Does not need to sign in.

### Administrator

- There is one administrator account in Phase 1.
- Signs in with email and password.
- Can upload image files and delete images.
- Can log out, which invalidates the server-side session.

## 4. Phase 1 scope

### In scope

This section is the Phase 1 product target. It is not a claim that every requirement is already present in the backend; see [Implementation status and roadmap](#10-implementation-status-and-roadmap) for the source-code state as of 2026-11-12.

- Public gallery of available images.
- Infinite scrolling with paged API requests: the initial request returns up to 10 images; each subsequent request also returns up to 10 images.
- Admin email/password login and logout.
- Authenticated admin image upload and deletion.
- Public Supabase Storage `gallery` bucket for image files. Each file uses the fixed object path `images/{imageId}`.
- Supabase Database (PostgreSQL) for application gallery records. Each record stores `imageId`, the business field `image_url`, and the system-managed `created_at` field; there are no title, caption, description, or other user-provided metadata fields.
- Redis-backed server-side sessions.
- Authorization, CSRF protection, cookie security settings, and server-side file validation.
- Accepted image formats are `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF`, with a maximum file size of 15 MB.
- Server-side sessions have a lifetime of 10 minutes and use session rotation.
- CSRF tokens are generated using `SecureRandom` and encoded using Base64 URL encoding.

### Confirmed API conventions

- The frontend obtains a CSRF token from `GET /api/v1/csrf-token`. The endpoint is public and has no request body, path variable, or query parameter.
- The CSRF endpoint stores the generated token in the `XSRF-TOKEN` cookie and does not return it in `data.token`. The frontend echoes the cookie value in the `X-XSRF-TOKEN` header for protected state-changing requests.
- API responses use a common envelope containing `status`, `code`, `message`, `timestamp`, and endpoint-specific `data` fields.
- Successful admin login and successful image upload return `201 Created`.
- Successful admin logout and successful image deletion return `200 OK`.
- Gallery pagination uses an opaque `nextCursor`. The initial request omits `nextCursor`; subsequent requests send the cursor returned by the previous response. Every request returns up to 10 images and includes `limit`, `nextCursor`, and `hasNextPage` pagination metadata.

### Out of scope for Phase 1

- Multiple administrator accounts or public user accounts.
- Image titles, captions, descriptions, or other image metadata.
- Editing image details, image downloads, or a separate image detail page.
- A gallery page-number interface; pagination is used to support infinite scrolling.
- Any image storage provider other than Supabase Storage.
- Login rate limiting is deferred and is not part of the current implementation.

## 5. User flows

### 5.1 Browse gallery

1. Visitor opens the website.
2. The gallery requests the first page of images (up to 10 items) without a `nextCursor`.
3. The visitor scrolls down.
4. The frontend requests each subsequent page with the returned opaque `nextCursor` and a page size of up to 10 until no more images are available.
5. The gallery displays the available images in `created_at DESC, id DESC` order without requiring sign-in.

### 5.2 Administrator login

1. Administrator submits email and password.
2. Backend verifies the credentials.
3. On success, backend creates a server-side session stored in Redis, sends the session cookie, and returns `201 Created` using the common response envelope.
4. The browser includes the cookie with subsequent requests; Spring Security authenticates the administrator and applies authorization rules.
5. On invalid credentials, the request is rejected without creating an authenticated session.

### 5.3 Upload an image

1. Authenticated administrator chooses an image file from their device.
2. The UI displays a temporary preview before submission.
3. Administrator submits the image.
4. Backend validates file size, allowed format, MIME type, and actual content/type.
5. Backend generates the `imageId` and fixed Storage object path `images/{imageId}`, then uploads the actual file to the public `gallery` bucket in Supabase Storage.
6. Backend obtains the resulting image URL and inserts the `imageId`, `image_url`, and system-generated `created_at` into `public.images`.
7. Backend returns `201 Created` using the common response envelope.
8. The image becomes available in the public gallery and appears before older images.

### 5.4 Delete an image

1. Authenticated administrator requests deletion using the image's `imageId`.
2. Backend authorizes the request and finds the corresponding row in `public.images`.
3. Backend derives the fixed Storage object path `images/{imageId}` and removes the file through the Supabase Storage API.
4. Backend removes the corresponding row from `public.images`.
5. Backend returns `200 OK` using the common response envelope.
6. The deleted image no longer appears in the public gallery.

### 5.5 Administrator logout

1. Authenticated administrator requests logout.
2. Backend invalidates the server-side session and clears or expires the session cookie.
3. Backend returns `200 OK` using the common response envelope. Subsequent protected requests are unauthenticated and denied.

## 6. Functional requirements

- **FR-01 Public access:** Visitors can retrieve and view gallery images without authentication.
- **FR-02 Initial page:** The first gallery request returns up to 10 images.
- **FR-03 Infinite scrolling:** The initial request omits `nextCursor`; subsequent requests send the opaque `nextCursor` returned by the previous response. Each request returns up to 10 images and stops when no further results exist.
- **FR-04 Admin login:** The system authenticates the single administrator using email and password.
- **FR-05 Session handling:** Successful login creates a server-side session in Redis with a 10-minute lifetime, applies session rotation, and returns a session cookie for subsequent requests.
- **FR-06 Admin authorization:** Only the authenticated administrator can upload or delete images.
- **FR-07 Upload preview:** The admin UI previews a selected local image before it is submitted.
- **FR-08 Image upload:** The backend generates `imageId`, validates the image, uploads the actual file to `images/{imageId}` in the public Supabase Storage `gallery` bucket, and persists `imageId`, the resulting `image_url`, and system-generated `created_at` in `public.images`.
- **FR-09 Image record:** The only business data for an image is `image_url`; `public.images` also stores `imageId` and system-managed `created_at`. No title, caption, description, or other user-provided metadata is collected.
- **FR-10 Image deletion:** The administrator can delete an image by `imageId`; the backend removes the corresponding Storage object through the Storage API and the database record.
- **FR-11 Logout:** The administrator can log out; the server-side session is invalidated.
- **FR-12 CSRF:** The frontend obtains a CSRF token from the public `GET /api/v1/csrf-token` endpoint. The backend stores it in the `XSRF-TOKEN` cookie; the frontend echoes it in the `X-XSRF-TOKEN` header for state-changing cookie-authenticated operations. CSRF tokens are generated using `SecureRandom` and encoded using Base64 URL encoding.
- **FR-13 Login rate limiting (deferred):** The target policy is to limit login requests to 3 requests per second; no rate-limiter implementation is currently present.
- **FR-14 Upload validation:** The backend accepts only `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` files up to 15 MB, and validates their actual content/type on the server.
- **FR-15 Response conventions:** API responses use the common `status`, `code`, `message`, `timestamp`, and `data` envelope. Successful login and image upload return `201 Created`; successful logout and image deletion return `200 OK`.

## 7. Security and data requirements

- Store the initial administrator account in the database, provisioned by a seed or migration.
- Store only a one-way password hash, never a plain-text password.
- Provide the initial password through secret configuration during provisioning; do not commit it to source control.
- Use Redis as the server-side session store.
- Set the server-side session lifetime to 10 minutes and apply session rotation.
- During local development, configure the session cookie as `HttpOnly; SameSite=Lax` with `Secure` disabled and `Domain` omitted (host-only). Configure the frontend-readable `XSRF-TOKEN` cookie with `Path=/; SameSite=Lax`, `Secure` disabled, and `Domain` omitted; it must not be `HttpOnly`. Enable `Secure` when deploying over HTTPS.
- Expose `GET /api/v1/csrf-token` as the public CSRF-token endpoint with no request body, path variable, or query parameter. Store the generated token in the `XSRF-TOKEN` cookie rather than in `data.token`.
- Generate CSRF tokens using `SecureRandom` and encode them using Base64 URL encoding.
- Login rate limiting is deferred; the removed prototype is not part of the current implementation scope.
- Validate uploaded files against the `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` allowlist, enforce the 15 MB maximum size, and inspect actual file content/type on the server. Do not rely only on filename extension or browser-provided MIME type.
- Supabase Storage stores the actual image files in the public `gallery` bucket at `images/{imageId}`; PostgreSQL stores application gallery records in `public.images`, including `imageId`, `image_url`, and system-managed `created_at`.
- Gallery queries order records with `ORDER BY created_at DESC, id DESC` and use the supporting index `CREATE INDEX idx_images_created_at ON public.images (created_at DESC);`.
- The image binary is not stored in PostgreSQL. Storage objects are managed through the Supabase Storage API, not by deleting rows directly from `storage.objects` with SQL.
- Credential storage is implemented through the admin entity and opt-in seed variables; production-specific cookie deployment values remain open. Deletion consistency is only partially resolved: current code uses Storage-first ordering, while a database failure after remote deletion requires reconciliation. UC04 upload compensation is defined as Storage-first, database-second, with Storage API cleanup after a failed database insert.

## 8. Acceptance criteria

- A visitor can view the gallery without logging in.
- The initial gallery load omits `nextCursor` and requests at most 10 images; scrolling requests later pages with the returned `nextCursor` and a limit of 10, ending when no more images are returned.
- Invalid admin credentials do not authenticate the user.
- A successful admin login returns `201 Created`, establishes a Redis-backed server-side session, and sends a browser cookie.
- Unauthenticated requests to upload or delete images are denied.
- A valid authenticated admin can select an image, see a preview, submit it, and receive `201 Created`; the resulting image appears before older images, with its `image_url` and `created_at` stored in `public.images` and its file stored in the `gallery` Storage bucket.
- Uploads over 15 MB or outside the `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` allowlist are rejected by server-side validation.
- Deleting an image using `imageId` returns `200 OK` and removes it from the gallery, the corresponding Storage object through the Storage API, and the corresponding database record.
- Logout returns `200 OK`, invalidates the session, and causes subsequent requests to protected operations to be denied.
- Cookie-authenticated state-changing requests are subject to CSRF protection.

## 9. Dependencies and architecture

- Frontend: Next.js and TypeScript.
- Backend: Java, Spring Boot, Spring Web MVC, Spring Security, Spring Data JPA, and Bean Validation.
- Persistence: Supabase Database (PostgreSQL) for gallery records (`image_url` and system-managed `created_at`) and the admin account.
- File storage: Supabase Storage.
- Session storage: Redis.
- Implemented test tools: JUnit 5, Mockito, and Testcontainers. Live external-service verification remains environment-dependent.

Java `21.0.12`, Maven Wrapper `3.10.0`, and Spring Boot `4.1.2` are recorded for the current backend. Node.js/frontend versions, deployment environments, remaining API paths, and external service configuration are still TBD.

## 10. Implementation status and roadmap

Status is based on the current source tree and tests, not on a live end-to-end run against PostgreSQL, Redis, or Supabase.

| Roadmap milestone | Status | Current evidence or remaining work |
| --- | --- | --- |
| API envelope, CSRF contract, and database schema | [x] Implemented | `ApiResponse`, `CsrfController`, `SecurityConfig`, and `V1_create_schemas.sql` are present. |
| Admin login and current-session lookup | [x] Implemented | `AdminController`, `AdminSessionService`, `AdminUserDetailsService`, and Redis-backed Spring Session are present. |
| Admin logout and session invalidation endpoint | [ ] Not implemented | No logout endpoint is present and logout is disabled in `SecurityConfig`. |
| Opt-in admin seed with BCrypt | [x] Implemented | `AdminSeedRunner` and `AdminSeedService` create the account idempotently without storing plaintext passwords. |
| Protected image upload | [x] Implemented | `POST /api/v1/images` is protected by role authorization and CSRF, then delegates to the upload service. |
| Upload validation and Storage/Database persistence | [x] Implemented | Allowlist, MIME/magic-byte/size checks, Supabase Storage adapter, database insert, and upload compensation are present. |
| Image deletion | [x] Implemented with limitation | `DELETE /api/v1/images/{imageId}` deletes Storage first and then PostgreSQL; a remote deletion followed by database failure requires reconciliation. |
| Public listing with opaque `nextCursor` | [ ] Not implemented | No listing controller/service or cursor pagination exists; the repository query is currently unpaginated. |
| Frontend preview, infinite scroll, and integration | [ ] Not implemented | No Next.js/TypeScript source or package scripts are present in this checkout. |
| Live external-service and HTTP-flow verification | [ ] Pending | Source-level tests exist, but live PostgreSQL/Redis/Supabase behavior is not established by this document update. |

### Roadmap checklist

- [x] Finalize the schema, common response envelope, CSRF contract, and backend security foundation.
- [x] Implement admin login, current-session lookup, Redis-backed sessions, and opt-in BCrypt seed.
- [x] Implement protected upload, validation, Supabase Storage integration, PostgreSQL persistence, and focused tests.
- [x] Implement protected image deletion with explicit Storage-first ordering and failure handling.
- [ ] Add admin logout and server-side session invalidation.
- [ ] Add public listing with 10-item opaque-cursor pagination and newest-first ordering.
- [ ] Add the frontend preview and infinite-scroll workflow.
- [ ] Reverify all implemented flows against live external services.

## 11. Open decisions

- Node.js, frontend, PostgreSQL, and Redis versions/configuration for the target environments; Java `21.0.12` and Spring Boot `4.1.2` are fixed for the current backend.
- Image naming/overwrite policy beyond the current generated UUID path.
- Public `gallery` bucket cache and access-policy configuration values.
- Production deployment values for the already implemented opt-in seed variables `ADMIN_SEED_ENABLED`, `ADMIN_EMAIL`, and `ADMIN_INITIAL_PASSWORD`.
- Production-specific session-cookie and CSRF-cookie deployment values, including whether a cross-subdomain `Domain` is needed and when `Secure` must be enabled.
- The exact opaque cursor encoding and endpoint-specific response schemas. The common response envelope, `nextCursor` pagination metadata, CSRF cookie delivery, public `gallery` bucket, and `images/{imageId}` Storage path are confirmed; the required page size is up to 10 images for the initial and subsequent requests.
- Behavior and recovery strategy for deletion remains open. Current code deletes Storage first and then PostgreSQL; a database failure after remote deletion still requires reconciliation. UC04 upload recovery is defined: upload Storage first, insert PostgreSQL second, then delete the Storage object through the Storage API if the database insert fails; cleanup failure requires reconciliation.
- Deployment and local development setup, including whether Supabase runs hosted or locally.
