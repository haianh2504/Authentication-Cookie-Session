# Use Cases — Authentication & Cookie Session

> **Project:** Ronaldo Photo Gallery  
> **Purpose:** Practice REST API design, Spring Boot request flow, Spring Security, cookie-based sessions, authorization, validation, and Supabase image-file handling.

## 1. Scope and assumptions

The website lets visitors browse a public gallery of Ronaldo photos. An administrator signs in and can upload and delete image files.

For image uploads, the admin selects a file from their device. The frontend sends the file to the backend as `multipart/form-data`. The backend generates an `imageId` before upload, uses the fixed Storage object path `images/{imageId}` inside the public `gallery` bucket, validates and uploads the actual image file, then stores the `imageId`, resulting `image_url`, and system-managed `created_at` in the application table `public.images`. The browser receives the image URL that can be used to display the image.

This is a learning project. Supabase Storage is the official Phase 1 image-file storage solution, and the `gallery` bucket is public for image delivery. The CSRF token cookie, fixed Storage object path, cursor field, common response envelope, and successful-operation status codes defined below are part of the confirmed API behavior. Other exact endpoint paths and remaining API-contract details are **deferred** until API design is completed.

## 2. Actors

| Actor | Description |
| --- | --- |
| Visitor | Unauthenticated website user who can view the public gallery. |
| Admin | Authorized administrator who can authenticate and manage gallery images. |
| Frontend | Next.js client that displays the gallery and submits admin actions. |
| Backend | Spring Boot REST API that authenticates, authorizes, validates, and processes requests. |

## 3. Use-case overview

| ID | Use case | Primary actor | Access |
| --- | --- | --- | --- |
| UC00 | Get a CSRF token | Frontend | Public |
| UC01 | Browse all gallery images | Visitor | Public |
| UC02 | Sign in as admin | Admin | Public endpoint; valid credentials required |
| UC03 | Check an existing admin session | Admin | Requires a valid session cookie |
| UC04 | Upload an image | Admin | Authenticated admin only |
| UC05 | Delete an image | Admin | Authenticated admin only |
| UC06 | Log out as admin | Admin | Authenticated admin only |

## UC00 — Get a CSRF token

**Goal:** Give the frontend a CSRF token for subsequent cookie-authenticated state-changing requests.

**Access:** Public.

**Endpoint:** `POST /api/v1/csrf-token`

**Request:** No request body, path variable, or query parameter.

**Success response:** `200 OK` using the common response envelope. The response sets an `XSRF-TOKEN` cookie; the token is not returned in `data.token`.

The token is generated with `SecureRandom` and encoded using Base64 URL encoding. During local development, the `XSRF-TOKEN` cookie uses `Path=/` and `SameSite=Lax`; `Secure` is not enabled and `Domain` is omitted, so the cookie is host-only. The cookie is readable by the frontend because it is not `HttpOnly`. The frontend reads the cookie and sends its value in the `X-XSRF-TOKEN` header on protected state-changing requests. The session cookie remains separate from the CSRF-token cookie.

## UC01 — Browse all gallery images

**Goal:** Display all available gallery images to a visitor.

**Preconditions:** The website and backend are available. No login is required.

**Main flow:**

1. Visitor opens the gallery page.
2. Frontend requests the gallery from the backend.
3. Backend retrieves image records from Supabase Database, ordered by `created_at DESC, id DESC` so newer images appear first.
4. The initial request omits `nextCursor` and returns up to 10 images.
5. Each subsequent infinite-scroll request sends the opaque `nextCursor` returned by the previous response and also returns up to 10 images until no more images are available.
6. Backend returns a `200 OK` response using the common response envelope. The response data contains the image items and pagination metadata, including `limit`, `nextCursor`, and `hasNextPage`.
7. Frontend loads and displays the images.

**Alternative flows:**

- **No images exist:** Backend returns `200 OK` with an empty collection (for example, `[]`). Frontend displays a friendly empty-gallery state.
- **Backend or storage error:** Backend returns an appropriate generic error response. Frontend displays a general loading/error state; internal details remain in server logs.

**Postconditions:** No data is changed.

## UC02 — Sign in as admin

**Goal:** Authenticate an admin and establish a server-side session.

**Preconditions:** Admin has valid credentials. The browser has no usable authenticated session.

**Main flow:**

1. Admin submits email and password through the login form.
2. Frontend sends the credentials to the login endpoint over HTTPS in deployed environments.
3. Backend validates required fields and authenticates the credentials using Spring Security.
4. Backend creates an authenticated server-side session.
5. Backend returns the session cookie with appropriate security attributes and a `201 Created` response using the common response envelope.
6. Browser stores the cookie and sends it with subsequent applicable requests.

**Alternative flows:**

- **Missing or malformed fields:** Backend returns `400 Bad Request` with safe validation feedback.
- **Well-formed but incorrect credentials:** Backend returns a generic `401 Unauthorized` response. It does not disclose whether the email or password was incorrect or whether an account exists.
- **Authentication service failure:** Backend returns a generic server error and records diagnostic details without logging the password or session identifier.

**Postconditions:** On success, the admin has an authenticated session. Do not claim that the login persists after closing the browser unless persistent “remember me” behavior is explicitly implemented.

## UC03 — Check an existing admin session

**Goal:** Let the frontend determine whether the browser's existing session still authenticates the admin.

**Preconditions:** Browser may have a session cookie from an earlier login.

**Main flow:**

1. Frontend makes a session-status or other protected request; browser attaches the cookie automatically.
2. Backend resolves the session and checks that it is valid, unexpired, and authenticated as an admin.
3. Backend returns a successful response indicating the authenticated state and only the safe user details the UI needs.
4. Frontend displays the admin state and allows access to admin controls.

**Alternative flows:**

- **Cookie absent, invalid, or expired:** Backend returns `401 Unauthorized` with a general message such as “Your session is invalid or has expired. Please sign in again.” Frontend returns to the login page.
- **Unexpected session failure:** Backend returns a generic response to the client and records the specific diagnostic reason server-side. Never log or return the raw session ID.

**Postconditions:** No data is changed. A stale cookie should be cleared where practical.

## UC04 — Upload an image

**Goal:** Let an authenticated admin add an image selected from their device to the gallery.

**Preconditions:** Admin has a valid authenticated session. The selected file is no larger than 15 MB and uses one of the allowed formats: `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, or `.AVIF`.

**Main flow:**

1. Admin selects an image file in the frontend.
2. Frontend sends a `multipart/form-data` request containing the file. The browser attaches the session cookie; the request also satisfies the configured CSRF protection.
3. Backend confirms the requester is authenticated and authorized as an admin.
4. Backend validates the upload, including size and allowed image content/type. Client-provided filename and MIME type alone are not trusted.
5. Backend generates the `imageId` and fixed Storage object path `images/{imageId}`, then uploads the file to the public `gallery` bucket in Supabase Storage.
6. Backend obtains the resulting image URL and inserts the `imageId`, `image_url`, and system-generated `created_at` into `public.images`.
7. Backend returns a `201 Created` response using the common response envelope and containing the new image URL.
8. Frontend refreshes or updates the gallery, which displays the newest images first.

**Alternative flows:**

- **Unauthenticated requester:** Return `401 Unauthorized`.
- **Authenticated requester without admin authority:** Return `403 Forbidden`.
- **Missing, empty, unsupported, or invalid image:** Return a suitable client error such as `400 Bad Request` or `415 Unsupported Media Type`.
- **File exceeds 15 MB or uses an unsupported format:** Return an appropriate client error. The exact response schema remains deferred to API-contract design.
- **Storage or database failure:** Return a generic server error, log diagnostic details safely, and avoid leaving an inconsistent Storage object or URL record. Define cleanup or transaction-compensation behavior.

**Postconditions:** On success, the image file is stored in Supabase Storage, its image URL is persisted in Supabase Database, and it is available to UC01.

## UC05 — Delete an image

**Goal:** Let an authenticated admin remove an image from the gallery.

**Preconditions:** Admin has a valid authenticated session. The target image exists.

**Main flow:**

1. Admin selects an image and confirms deletion.
2. Frontend sends a delete request identifying the selected image. The browser attaches the session cookie and the request satisfies CSRF protection.
3. Backend verifies authentication and admin authorization.
4. Backend finds the `public.images` row using the supplied `imageId`, derives the fixed Storage object path `images/{imageId}`, and removes the image file through the Supabase Storage API and the row from `public.images`, following the defined consistency strategy.
5. Backend returns a `200 OK` response using the common response envelope.
6. Frontend removes the image from the displayed gallery.

**Alternative flows:**

- **Unauthenticated requester:** Return `401 Unauthorized`.
- **Authenticated requester without admin authority:** Return `403 Forbidden`.
- **Image does not exist:** Return `404 Not Found` (or document an intentionally idempotent delete policy).
- **Storage or database deletion failure:** Return a generic server error and log details. Define how the application reconciles a missing Storage object or orphaned URL record.

**Postconditions:** On success, the image no longer appears in the gallery, its Supabase Storage object is removed, and its URL record is removed according to the deletion policy.

## 4. Cross-cutting rules

- API responses use a common envelope containing `status`, `code`, `message`, `timestamp`, and endpoint-specific `data` fields.
- Successful admin login and successful image upload return `201 Created`.
- Successful admin logout and successful image deletion return `200 OK`.
- Public gallery reads do not require authentication.
- Upload and delete operations require admin authorization.
- Cookie-based authentication must use a documented CSRF defense for state-changing requests. The CSRF token is stored in the `XSRF-TOKEN` cookie and echoed in the `X-XSRF-TOKEN` header.
- During local development, the session cookie uses `HttpOnly`, `SameSite=Lax`, omits `Secure`, and omits `Domain` so it is host-only. `Secure` must be enabled when deploying over HTTPS.
- The `XSRF-TOKEN` cookie must be available to the frontend so it can echo the token in `X-XSRF-TOKEN`; the authentication session cookie remains separate.
- Never log passwords, raw session IDs, or sensitive cookie values.
- Validate uploaded content on the server. Do not trust the original filename, extension, or browser-supplied content type.
- Use the fixed server-generated Storage object path `images/{imageId}` inside the public `gallery` bucket; do not use a user-supplied path as a Storage path.
- Accept only `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` files, enforce a maximum upload size of 15 MB, and validate actual file content/type on the server.
- Supabase Storage stores the actual image file in the public `gallery` bucket; `public.images` stores the application gallery record, including `imageId`, `image_url`, and system-managed `created_at`.
- Keep Supabase Storage objects and database gallery records consistent when uploads or deletions partially fail.
- Delete Storage objects through the Supabase Storage API, not by deleting rows directly from `storage.objects` with SQL.
- Delete requests identify images with `imageId`, not `imageURL`.
- Server-side sessions are stored in Redis, have a lifetime of 10 minutes, and use session rotation.
- Generate CSRF tokens using `SecureRandom` and encode them using Base64 URL encoding.
- Limit login requests to 3 requests per second.

## 5. Open design decisions

- Exact REST endpoint paths for operations other than the confirmed CSRF endpoint, and endpoint-specific request/response schemas.
- Whether UC03 uses a dedicated session-status endpoint or relies on normal protected requests.
- Production-specific cookie deployment values, such as when to enable `Secure` and whether a cross-subdomain `Domain` is required.
- Gallery pagination uses an opaque `nextCursor`: the initial request omits it, and each subsequent request sends the cursor returned by the previous response. The initial and subsequent requests return up to 10 images, ordered by newest `created_at` first. The response includes `limit`, `nextCursor`, and `hasNextPage` pagination metadata.
- Storage/database consistency and cleanup strategy for failed uploads and deletions.
- Other API-contract details and production-specific session-cookie deployment values.

## 6. Additional Phase 1 requirements

- Phase 1 has one administrator account. Multiple administrator accounts and public user accounts are out of scope.
- Gallery loading uses infinite scrolling. The initial request returns up to 10 images; subsequent requests also return up to 10 images per page and stop when no more images are available.
- Before an upload is submitted, the frontend displays a temporary preview of the selected local image.
- The only business data stored for a Phase 1 image is `image_url`. The `public.images` table also contains the system-managed `created_at` field. Image titles, captions, descriptions, and other user-provided metadata are out of scope.
- The `created_at` field uses `TIMESTAMPTZ NOT NULL DEFAULT now()` and supports newest-first gallery ordering with `ORDER BY created_at DESC, id DESC`.
- The supporting database index is `CREATE INDEX idx_images_created_at ON public.images (created_at DESC);`.
- The upload flow generates `imageId`, stores the actual image file at `images/{imageId}` in the public Supabase Storage `gallery` bucket, and stores the `imageId` and `image_url` in `public.images`. The image binary is not stored in PostgreSQL.
- Server-side sessions are stored in Redis.
- The initial administrator account is provisioned through a seed or migration. Only a one-way password hash is stored, and the initial password is supplied through secret configuration rather than committed to source control.

## UC06 — Log out as admin

**Goal:** Let the authenticated administrator end the current session.

**Preconditions:** Admin has a valid authenticated session.

**Main flow:**

1. Admin requests logout from the frontend.
2. Backend invalidates the server-side session stored in Redis.
3. Backend clears or expires the session cookie.
4. Backend returns a `200 OK` response using the common response envelope.
5. Frontend updates the UI to the unauthenticated state.

**Postconditions:** Subsequent requests to protected operations are unauthenticated and denied. The successful logout response is `200 OK` using the common response envelope.
