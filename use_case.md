# Use Cases — Authentication & Cookie Session

> **Project:** Ronaldo Photo Gallery  
> **Purpose:** Practice REST API design, Spring Boot request flow, Spring Security, cookie-based sessions, authorization, validation, and Supabase image-file handling.

## 1. Scope and assumptions

The website lets visitors browse a public gallery of Ronaldo photos. An administrator signs in and can upload and delete image files.

For image uploads, the admin selects a file from their device. The frontend sends the file to the backend as `multipart/form-data`. The backend validates and uploads the file to Supabase Storage, then stores only the resulting image URL in Supabase Database (PostgreSQL). The browser receives the image URL that can be used to display the image.

This is a learning project. Supabase Storage is the official Phase 1 image-file storage solution. Exact endpoint paths, response envelope, Supabase bucket access policy, and other API-contract details are **deferred** until API design is completed.

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
| UC01 | Browse all gallery images | Visitor | Public |
| UC02 | Sign in as admin | Admin | Public endpoint; valid credentials required |
| UC03 | Check an existing admin session | Admin | Requires a valid session cookie |
| UC04 | Upload an image | Admin | Authenticated admin only |
| UC05 | Delete an image | Admin | Authenticated admin only |
| UC06 | Log out as admin | Admin | Authenticated admin only |

## UC01 — Browse all gallery images

**Goal:** Display all available gallery images to a visitor.

**Preconditions:** The website and backend are available. No login is required.

**Main flow:**

1. Visitor opens the gallery page.
2. Frontend requests the gallery from the backend.
3. Backend retrieves image URL records from Supabase Database.
4. Backend returns a successful response containing the available image URLs.
5. Frontend loads and displays the images.

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
5. Backend returns the session cookie with appropriate security attributes and a successful response.
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
5. Backend generates a safe server-side storage object name and stores the file in Supabase Storage.
6. Backend stores only the resulting image URL in Supabase Database.
7. Backend returns a success response containing the new image URL.
8. Frontend refreshes or updates the gallery.

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
4. Backend finds the image URL record and removes the image from the gallery, its Supabase Storage object, and its URL record, following the defined consistency strategy.
5. Backend returns a success response.
6. Frontend removes the image from the displayed gallery.

**Alternative flows:**

- **Unauthenticated requester:** Return `401 Unauthorized`.
- **Authenticated requester without admin authority:** Return `403 Forbidden`.
- **Image does not exist:** Return `404 Not Found` (or document an intentionally idempotent delete policy).
- **Storage or database deletion failure:** Return a generic server error and log details. Define how the application reconciles a missing Storage object or orphaned URL record.

**Postconditions:** On success, the image no longer appears in the gallery, its Supabase Storage object is removed, and its URL record is removed according to the deletion policy.

## 4. Cross-cutting rules

- Public gallery reads do not require authentication.
- Upload and delete operations require admin authorization.
- Cookie-based authentication must use a documented CSRF defense for state-changing requests.
- Session cookies should be configured securely (for example, `HttpOnly`, `Secure` when using HTTPS, and an appropriate `SameSite` value).
- Never log passwords, raw session IDs, or sensitive cookie values.
- Validate uploaded content on the server. Do not trust the original filename, extension, or browser-supplied content type.
- Use a safe server-side Supabase Storage object name; do not use a user-supplied path as a storage path.
- Accept only `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` files, enforce a maximum upload size of 15 MB, and validate actual file content/type on the server.
- Keep Supabase Storage objects and database URL records consistent when uploads or deletions partially fail.
- Server-side sessions are stored in Redis, have a lifetime of 10 minutes, and use session rotation.
- Generate CSRF tokens using `SecureRandom` and encode them using Base64 URL encoding.
- Limit login requests to 3 requests per second.

## 5. Open design decisions

- Exact REST endpoint paths and request/response schemas.
- Whether UC03 uses a dedicated session-status endpoint or relies on normal protected requests.
- Supabase Storage bucket access policy and whether image delivery uses public access or signed URLs.
- Ordering and pagination behavior for UC01; API pagination conventions and response schemas remain deferred to API-contract design.
- Storage/database consistency and cleanup strategy for failed uploads and deletions.
- Session cookie configuration, CSRF token delivery to the frontend, and other API-contract details.

## 6. Additional Phase 1 requirements

- Phase 1 has one administrator account. Multiple administrator accounts and public user accounts are out of scope.
- Gallery loading uses infinite scrolling. The initial request returns up to 10 images; subsequent requests return up to 20 images per page and stop when no more images are available.
- Before an upload is submitted, the frontend displays a temporary preview of the selected local image.
- Phase 1 image records contain the image URL only. Image titles, captions, descriptions, and other image metadata are out of scope.
- Server-side sessions are stored in Redis.
- The initial administrator account is provisioned through a seed or migration. Only a one-way password hash is stored, and the initial password is supplied through secret configuration rather than committed to source control.

## UC06 — Log out as admin

**Goal:** Let the authenticated administrator end the current session.

**Preconditions:** Admin has a valid authenticated session.

**Main flow:**

1. Admin requests logout from the frontend.
2. Backend invalidates the server-side session stored in Redis.
3. Backend clears or expires the session cookie.
4. Frontend updates the UI to the unauthenticated state.

**Postconditions:** Subsequent requests to protected operations are unauthenticated and denied.
