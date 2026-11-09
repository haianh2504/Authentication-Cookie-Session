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

- Public gallery of available images.
- Infinite scrolling with paged API requests: the initial request returns 10 images; each subsequent request returns up to 20 images.
- Admin email/password login and logout.
- Authenticated admin image upload and deletion.
- Supabase Storage for image files.
- Supabase Database (PostgreSQL) for image records. Each record stores the image URL only; there are no title or description fields.
- Redis-backed server-side sessions.
- Authorization, CSRF protection, cookie security settings, and server-side file validation.
- Accepted image formats are `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF`, with a maximum file size of 15 MB.
- Server-side sessions have a lifetime of 10 minutes and use session rotation.
- CSRF tokens are generated using `SecureRandom` and encoded using Base64 URL encoding.
- Login requests are limited to 3 requests per second.

### Out of scope for Phase 1

- Multiple administrator accounts or public user accounts.
- Image titles, captions, descriptions, or other image metadata.
- Editing image details, image downloads, or a separate image detail page.
- A gallery page-number interface; pagination is used to support infinite scrolling.
- Any image storage provider other than Supabase Storage.

## 5. User flows

### 5.1 Browse gallery

1. Visitor opens the website.
2. The gallery requests the first page of images (10 items).
3. The visitor scrolls down.
4. The frontend requests the next page, with a page size of 20, until no more images are available.
5. The gallery displays the available images without requiring sign-in.

### 5.2 Administrator login

1. Administrator submits email and password.
2. Backend verifies the credentials.
3. On success, backend creates a server-side session stored in Redis and sends the session cookie.
4. The browser includes the cookie with subsequent requests; Spring Security authenticates the administrator and applies authorization rules.
5. On invalid credentials, the request is rejected without creating an authenticated session.

### 5.3 Upload an image

1. Authenticated administrator chooses an image file from their device.
2. The UI displays a temporary preview before submission.
3. Administrator submits the image.
4. Backend validates file size and actual content/type, uploads the file to Supabase Storage, and stores the resulting image URL in Supabase Database.
5. The image becomes available in the public gallery.

### 5.4 Delete an image

1. Authenticated administrator requests deletion of an image.
2. Backend authorizes the request and removes the corresponding image file from Supabase Storage and its URL record from Supabase Database.
3. The deleted image no longer appears in the public gallery.

### 5.5 Administrator logout

1. Authenticated administrator requests logout.
2. Backend invalidates the server-side session and clears or expires the session cookie.
3. Subsequent protected requests are unauthenticated and denied.

## 6. Functional requirements

- **FR-01 Public access:** Visitors can retrieve and view gallery images without authentication.
- **FR-02 Initial page:** The first gallery request returns up to 10 images.
- **FR-03 Infinite scrolling:** When more images are needed, the frontend requests additional pages of up to 20 images and stops when no further results exist.
- **FR-04 Admin login:** The system authenticates the single administrator using email and password.
- **FR-05 Session handling:** Successful login creates a server-side session in Redis with a 10-minute lifetime, applies session rotation, and returns a session cookie for subsequent requests.
- **FR-06 Admin authorization:** Only the authenticated administrator can upload or delete images.
- **FR-07 Upload preview:** The admin UI previews a selected local image before it is submitted.
- **FR-08 Image upload:** The backend validates and uploads the image file to Supabase Storage, then persists its URL in Supabase Database.
- **FR-09 Image record:** An image record contains the image URL only; no title or description is collected.
- **FR-10 Image deletion:** The administrator can delete an image; its file and database record are removed.
- **FR-11 Logout:** The administrator can log out; the server-side session is invalidated.
- **FR-12 CSRF:** State-changing cookie-authenticated operations are protected by a configured CSRF strategy. CSRF tokens are generated using `SecureRandom` and encoded using Base64 URL encoding.
- **FR-13 Login rate limiting:** Login requests are limited to 3 requests per second.
- **FR-14 Upload validation:** The backend accepts only `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` files up to 15 MB, and validates their actual content/type on the server.

## 7. Security and data requirements

- Store the initial administrator account in the database, provisioned by a seed or migration.
- Store only a one-way password hash, never a plain-text password.
- Provide the initial password through secret configuration during provisioning; do not commit it to source control.
- Use Redis as the server-side session store.
- Set the server-side session lifetime to 10 minutes and apply session rotation.
- Configure session cookie attributes appropriately: `HttpOnly`, `Secure` for HTTPS deployments, and a suitable `SameSite` policy.
- Generate CSRF tokens using `SecureRandom` and encode them using Base64 URL encoding.
- Limit login requests to 3 requests per second.
- Validate uploaded files against the `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` allowlist, enforce the 15 MB maximum size, and inspect actual file content/type on the server. Do not rely only on filename extension or browser-provided MIME type.
- Supabase Storage stores image files; Supabase Database stores image URLs only.
- Credential storage, CSRF token exchange details, Storage bucket access policy, and deletion consistency behavior remain to be specified during implementation.

## 8. Acceptance criteria

- A visitor can view the gallery without logging in.
- The initial gallery load requests at most 10 images; scrolling requests later pages with a limit of 20 and ends when no more images are returned.
- Invalid admin credentials do not authenticate the user.
- A successful admin login establishes a Redis-backed server-side session and a browser cookie.
- Unauthenticated requests to upload or delete images are denied.
- A valid authenticated admin can select an image, see a preview, submit it, and the resulting image appears in the gallery with its URL stored in Supabase Database and file in Supabase Storage.
- Uploads over 15 MB or outside the `.jpg`, `.jpeg`, `.png`, `.HEIC`, `.HEIF`, `.WebP`, and `.AVIF` allowlist are rejected by server-side validation.
- Deleting an image removes it from the gallery and the corresponding Storage object and database record.
- Logout invalidates the session; subsequent requests to protected operations are denied.
- Cookie-authenticated state-changing requests are subject to CSRF protection.

## 9. Dependencies and architecture

- Frontend: Next.js and TypeScript.
- Backend: Java, Spring Boot, Spring Web MVC, Spring Security, Spring Data JPA, and Bean Validation.
- Persistence: Supabase Database (PostgreSQL) for image URL records and the admin account.
- File storage: Supabase Storage.
- Session storage: Redis.
- Planned test tools: JUnit 5, Mockito, and Testcontainers.

Exact versions, API paths/contracts, deployment environments, and service configuration are TBD.

## 10. Open decisions

- Exact Java, Node.js, Spring Boot, PostgreSQL, and Redis versions.
- Image naming/overwrite policy.
- Whether Supabase Storage objects are public or accessed through signed URLs.
- Exact initial admin provisioning mechanism and secret variable names.
- Session cookie configuration values and CSRF token exchange flow.
- API pagination convention (page number, offset, or cursor) and response schema.
- Behavior and recovery strategy if Storage upload/deletion succeeds but the database operation fails, or vice versa.
- Deployment and local development setup, including whether Supabase runs hosted or locally.
