# Use Cases — Authentication & Cookie Session

> **Project:** Ronaldo Photo Gallery  
> **Purpose:** Practice REST API design, Spring Boot request flow, Spring Security, cookie-based sessions, authorization, validation, and local image-file handling.

## 1. Scope and assumptions

The website lets visitors browse a public gallery of Ronaldo photos. An administrator signs in and can upload and delete image files.

For image uploads, the admin selects a file from their device. The frontend sends the file to the backend as `multipart/form-data`. The backend validates and stores the file on its local filesystem, then stores image metadata and a reference to the stored file in PostgreSQL. The browser receives a URL or API route that can be used to display the image; the uploaded file itself is not stored as a database URL string.

This is a local learning project. Local filesystem storage is suitable for development, but deployment behavior, backup, and shared storage are not defined here. Exact endpoint paths, response envelope, image size limit, supported formats, and filesystem location are **to be defined** during API design.

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

## UC01 — Browse all gallery images

**Goal:** Display all available gallery images to a visitor.

**Preconditions:** The website and backend are available. No login is required.

**Main flow:**

1. Visitor opens the gallery page.
2. Frontend requests the gallery from the backend.
3. Backend retrieves image metadata for the available images from PostgreSQL.
4. Backend returns a successful response containing image identifiers, display metadata, and image URLs or resource paths.
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

**Preconditions:** Admin has a valid authenticated session. The selected file meets the defined size and format rules.

**Main flow:**

1. Admin selects an image file in the frontend.
2. Frontend sends a `multipart/form-data` request containing the file and any defined metadata. The browser attaches the session cookie; the request also satisfies the configured CSRF protection.
3. Backend confirms the requester is authenticated and authorized as an admin.
4. Backend validates the upload, including size and allowed image content/type. Client-provided filename and MIME type alone are not trusted.
5. Backend generates a safe server-side filename and stores the file under the configured local upload directory, outside the public web root where applicable.
6. Backend stores metadata and a safe file reference in PostgreSQL.
7. Backend returns a success response with the new image's identifier and display resource URL/path.
8. Frontend refreshes or updates the gallery.

**Alternative flows:**

- **Unauthenticated requester:** Return `401 Unauthorized`.
- **Authenticated requester without admin authority:** Return `403 Forbidden`.
- **Missing, empty, unsupported, or invalid image:** Return a suitable client error such as `400 Bad Request` or `415 Unsupported Media Type`.
- **File exceeds configured limit:** Return `413 Payload Too Large`.
- **Storage or database failure:** Return a generic server error, log diagnostic details safely, and avoid leaving an inconsistent file/metadata state. Define cleanup or transaction-compensation behavior.

**Postconditions:** On success, the image file is stored locally, its metadata is persisted, and it is available to UC01.

## UC05 — Delete an image

**Goal:** Let an authenticated admin remove an image from the gallery.

**Preconditions:** Admin has a valid authenticated session. The target image exists.

**Main flow:**

1. Admin selects an image and confirms deletion.
2. Frontend sends a delete request for the image identifier. The browser attaches the session cookie and the request satisfies CSRF protection.
3. Backend verifies authentication and admin authorization.
4. Backend finds the image record and removes the image from the gallery and its stored file, following the defined consistency strategy.
5. Backend returns a success response.
6. Frontend removes the image from the displayed gallery.

**Alternative flows:**

- **Unauthenticated requester:** Return `401 Unauthorized`.
- **Authenticated requester without admin authority:** Return `403 Forbidden`.
- **Image does not exist:** Return `404 Not Found` (or document an intentionally idempotent delete policy).
- **File or database deletion failure:** Return a generic server error and log details. Define how the application reconciles a missing file or orphaned metadata.

**Postconditions:** On success, the image no longer appears in the gallery and its stored file is removed according to the deletion policy.

## 4. Cross-cutting rules

- Public gallery reads do not require authentication.
- Upload and delete operations require admin authorization.
- Cookie-based authentication must use a documented CSRF defense for state-changing requests.
- Session cookies should be configured securely (for example, `HttpOnly`, `Secure` when using HTTPS, and an appropriate `SameSite` value).
- Never log passwords, raw session IDs, or sensitive cookie values.
- Validate uploaded content on the server. Do not trust the original filename, extension, or browser-supplied content type.
- Store generated filenames and metadata; avoid using a user-supplied path as a filesystem path.
- Define limits for upload size and supported formats, and constrain storage paths to prevent path traversal.
- Keep database records and filesystem contents consistent when uploads or deletions partially fail.

## 5. Open design decisions

- Exact REST endpoint paths and request/response schemas.
- Whether UC03 uses a dedicated session-status endpoint or relies on normal protected requests.
- Supported image formats and maximum upload size.
- Local upload directory configuration and whether image delivery uses a static resource route or a controller endpoint.
- Metadata fields stored in PostgreSQL and ordering/pagination behavior for UC01.
- File/database consistency and cleanup strategy for failed uploads and deletions.
- Session lifetime, Redis-backed session configuration, logout behavior, and CSRF token delivery to the frontend.
