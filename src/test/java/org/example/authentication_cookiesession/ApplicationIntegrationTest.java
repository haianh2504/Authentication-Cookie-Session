package org.example.authentication_cookiesession;

import jakarta.servlet.http.Cookie;
import org.example.authentication_cookiesession.entity.Admin;
import org.example.authentication_cookiesession.entity.Image;
import org.example.authentication_cookiesession.exception.ImageStorageException;
import org.example.authentication_cookiesession.repository.AdminRepository;
import org.example.authentication_cookiesession.repository.ImageRepository;
import org.example.authentication_cookiesession.storage.ImageStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/**
 * Kiểm tra các application flow qua Spring context, filter chain, PostgreSQL
 * và Redis thật; Supabase Storage được thay bằng Mockito mock có hành vi hợp đồng.
 */
// Khởi động toàn bộ application context thay vì chỉ tạo controller đơn lẻ.
@SpringBootTest
// Tạo MockMvc có đầy đủ Spring MVC và Spring Security filter chain.
@AutoConfigureMockMvc
// Khởi động PostgreSQL và Redis Testcontainers cho mỗi application context test.
@Import(TestcontainersConfiguration.class)
class ApplicationIntegrationTest {

    private static final String ADMIN_EMAIL = "integration-admin@example.com";
    private static final String ADMIN_PASSWORD = "integration-password";
    private static final String SESSION_COOKIE_NAME = "ADMIN_SESSION";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x01, 0x02
    };

    // MockMvc gửi request qua controller, security filter và service thật.
    @Autowired
    private MockMvc mockMvc;

    // Repository thật ghi và đọc PostgreSQL Testcontainer.
    @Autowired
    private AdminRepository adminRepository;

    // Repository thật kiểm tra kết quả upload/delete trong PostgreSQL.
    @Autowired
    private ImageRepository imageRepository;

    // JdbcTemplate tạo dữ liệu có created_at cố định để kiểm tra pagination boundary.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ObjectMapper đọc cursor từ response mà không tự giải mã cursor trong test contract.
    @Autowired
    private ObjectMapper objectMapper;

    // PasswordEncoder thật tạo BCrypt hash giống runtime production.
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Thay adapter Supabase thật bằng mock trong Spring application context.
    @MockitoBean
    private ImageStorage imageStorage;

    /**
     * Xóa dữ liệu của test trước mỗi flow và tạo lại admin bằng BCrypt hash.
     */
    // Chạy fixture trước từng test để test độc lập với thứ tự thực thi.
    @BeforeEach
    void setUpDatabaseAndStorage() {
        imageRepository.deleteAllInBatch();
        adminRepository.deleteAllInBatch();
        adminRepository.saveAndFlush(
                Admin.create(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD)));

        reset(imageStorage);
        when(imageStorage.upload(any(UUID.class), any(MultipartFile.class)))
                .thenAnswer(invocation -> {
                    UUID imageId = invocation.getArgument(0);
                    String storagePath = "images/" + imageId;
                    String publicUrl = "https://fake-storage.test/gallery/" + storagePath;
                    return new ImageStorage.StoredImage(storagePath, publicUrl);
                });
    }

    /**
     * Xóa SecurityContext của thread test để không làm rò authentication sang flow sau.
     */
    // Dọn context bảo mật sau mỗi request sequence.
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * UC00 phải phát cookie CSRF frontend đọc được và không trả token trong body.
     */
    // Kiểm tra endpoint cấp CSRF token công khai theo contract.
    @Test
    void csrfTokenEndpointIssuesReadableCookieWithoutReturningTokenInBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_RETRIEVING_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);

        assertNotNull(csrfCookie);
        assertNotNull(csrfCookie.getValue());
        assertFalse(csrfCookie.getValue().isBlank());
        assertFalse(csrfCookie.isHttpOnly());
    }

    /**
     * UC02 và UC03 phải tạo session cookie, lưu authentication trong Redis,
     * rồi khôi phục được trạng thái admin ở request tiếp theo.
     */
    // Kiểm tra login và session lookup qua filter chain thật.
    @Test
    void loginCreatesRedisBackedSessionAndCurrentSessionLookupConfirmsAdmin() throws Exception {
        Cookie sessionCookie = loginAsAdmin();

        mockMvc.perform(get("/api/v1/auth/admin/sessions")
                        .cookie(sessionCookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_RETRIEVING_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    /**
     * UC03 phải từ chối visitor không có session admin.
     */
    // Kiểm tra authorization của endpoint current-session khi thiếu cookie.
    @Test
    void currentSessionWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/sessions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * UC03 phải từ chối session cookie không tồn tại trong Redis.
     */
    // Kiểm tra cookie bị giả mạo hoặc đã hết hạn không tạo authentication giả.
    @Test
    void currentSessionWithUnknownCookieIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin/sessions")
                        .cookie(new Cookie(SESSION_COOKIE_NAME, "unknown-session-id"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * Login sai credentials phải trả 401 và không tạo session thành công.
     */
    // Kiểm tra lỗi credentials theo UC02 mà không làm lộ email/password nào.
    @Test
    void invalidLoginReturnsContractUnauthorizedResponse() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andReturn();

        assertNull(result.getResponse().getCookie(SESSION_COOKIE_NAME));
    }

    /**
     * UC01 phải trả gallery rỗng và metadata nhất quán khi chưa có image record.
     */
    // Kiểm tra boundary không có dữ liệu qua full Spring context và PostgreSQL thật.
    @Test
    void publicGalleryReturnsEmptyPageWhenNoImagesExist() throws Exception {
        mockMvc.perform(get("/api/v1/images")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.pagination.limit").value(10))
                .andExpect(jsonPath("$.data.pagination.nextCursor").value(nullValue()))
                .andExpect(jsonPath("$.data.pagination.hasNextPage").value(false));
    }

    /**
     * UC01 phải không tạo nextCursor khi tổng số ảnh đúng bằng page size.
     */
    // Kiểm tra boundary đúng 10 record, không được báo còn trang kế tiếp.
    @Test
    void publicGalleryReturnsExactlyTenImagesWithoutNextPage() throws Exception {
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        for (int index = 0; index < 10; index++) {
            insertImageWithCreatedAt(index + 1, baseTime.plusMinutes(index));
        }

        mockMvc.perform(get("/api/v1/images")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(10)))
                .andExpect(jsonPath("$.data.pagination.nextCursor").value(nullValue()))
                .andExpect(jsonPath("$.data.pagination.hasNextPage").value(false));
    }

    /**
     * UC01 phải chia đúng trang khi có 11 ảnh và trang sau không lặp record.
     */
    // Kiểm tra fetch 11, cursor opaque và request trang kế tiếp qua HTTP thật.
    @Test
    void publicGalleryReturnsNextPageForElevenImages() throws Exception {
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        for (int index = 0; index < 11; index++) {
            insertImageWithCreatedAt(index + 1, baseTime.plusMinutes(index));
        }

        MvcResult firstPage = mockMvc.perform(get("/api/v1/images")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(10)))
                .andExpect(jsonPath("$.data.pagination.hasNextPage").value(true))
                .andReturn();

        JsonNode firstPageJson = objectMapper.readTree(firstPage.getResponse().getContentAsString());
        String nextCursor = firstPageJson.path("data").path("pagination").path("nextCursor").asText();
        assertFalse(nextCursor.isBlank());

        mockMvc.perform(get("/api/v1/images")
                        .param("nextCursor", nextCursor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].imageId").value(imageId(1).toString()))
                .andExpect(jsonPath("$.data.pagination.nextCursor").value(nullValue()))
                .andExpect(jsonPath("$.data.pagination.hasNextPage").value(false));
    }

    /**
     * UC01 phải trả lỗi generic an toàn nếu nextCursor không giải mã được.
     */
    // Kiểm tra cursor rác không được làm lộ stack trace hoặc lỗi database.
    @Test
    void publicGalleryRejectsMalformedCursorWithSafeError() throws Exception {
        mockMvc.perform(get("/api/v1/images")
                        .param("nextCursor", "not-a-valid-cursor")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * UC01 phải xử lý cursor có Base64 hợp lệ nhưng payload không đúng schema.
     */
    // Kiểm tra lớp parse timestamp/UUID chứ không chỉ kiểm tra lỗi Base64.
    @Test
    void publicGalleryRejectsStructurallyInvalidCursorWithSafeError() throws Exception {
        String invalidCursor = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("2026-01-01T10:00:00Z|not-a-uuid"
                        .getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/images")
                        .param("nextCursor", invalidCursor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * UC01 phải coi cursor rỗng như request trang đầu thay vì query trang sau.
     */
    // Kiểm tra client gửi query parameter rỗng không làm hỏng gallery.
    @Test
    void publicGalleryTreatsBlankCursorAsFirstPage() throws Exception {
        insertImageWithCreatedAt(1, OffsetDateTime.parse("2026-01-01T10:00:00Z"));

        mockMvc.perform(get("/api/v1/images")
                        .param("nextCursor", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].imageId").value(imageId(1).toString()));
    }

    /**
     * UC02 phải trả lỗi validation cho email sai và password rỗng.
     */
    // Kiểm tra Bean Validation trước khi AuthenticationManager được gọi.
    @Test
    void loginRejectsInvalidEmailAndBlankPassword() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .content("""
                                {"email":"not-an-email","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors", hasSize(2)));
    }

    /**
     * UC02 phải báo lỗi validation riêng cho request JSON thiếu cả hai field.
     */
    // Kiểm tra object JSON rỗng không được chuyển thành authentication attempt.
    @Test
    void loginRejectsMissingRequiredFieldsWithValidationError() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors", hasSize(2)));
    }

    /**
     * UC02 phải chuyển malformed JSON thành lỗi 400 theo contract.
     */
    // Kiểm tra request body không parse được không rơi xuống lỗi 500 generic.
    @Test
    void loginRejectsMalformedJsonWithBadRequest() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    /**
     * UC02 phải dùng cùng lỗi 401 cho email không tồn tại và không tạo session.
     */
    // Kiểm tra chống account enumeration và session không được tạo khi login fail.
    @Test
    void loginWithUnknownEmailReturnsGenericUnauthorizedWithoutSession() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .content("""
                                {"email":"missing@example.com","password":"any-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andReturn();

        assertNull(result.getResponse().getCookie(SESSION_COOKIE_NAME));
    }

    /**
     * UC02 phải bị CSRF filter chặn nếu login không có token.
     */
    // Kiểm tra login public vẫn phải qua CSRF trước khi xác thực credentials.
    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_FORBIDDEN"));
    }

    /**
     * UC02 phải bị CSRF filter chặn nếu header không khớp với cookie.
     */
    // Kiểm tra double-submit cookie không chấp nhận token giả.
    @Test
    void loginWithInvalidCsrfTokenIsRejected() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, "invalid-token")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_FORBIDDEN"));
    }

    /**
     * UC04 phải bị CSRF filter chặn trước khi gọi Storage nếu header bị thiếu.
     */
    // Kiểm tra state-changing request không được bypass CSRF.
    @Test
    void uploadWithoutCsrfHeaderIsRejectedBeforeStorageCall() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        MockMultipartFile imageFile = validPngFile();

        mockMvc.perform(multipart("/api/v1/images")
                        .file(imageFile)
                        .cookie(sessionCookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC04 phải từ chối token CSRF không khớp trước khi chạm vào Storage hoặc database.
     */
    // Kiểm tra double-submit token giả không thể bypass lớp bảo vệ upload.
    @Test
    void uploadWithInvalidCsrfTokenIsRejectedBeforeStorageCall() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(multipart("/api/v1/images")
                        .file(validPngFile())
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, "invalid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_FORBIDDEN"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC04 phải trả 401 nếu request không có session admin hợp lệ.
     */
    // Kiểm tra authorization trước khi service có thể gọi Storage.
    @Test
    void uploadWithoutAdminSessionIsRejected() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(multipart("/api/v1/images")
                        .file(validPngFile())
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC04 phải upload Storage trước, lưu URL/id vào PostgreSQL sau và trả đúng envelope.
     */
    // Kiểm tra upload qua security, validator, service, mock Storage và repository thật.
    @Test
    void uploadUsesGeneratedStoragePathAndPersistsDatabaseRecord() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        MvcResult result = mockMvc.perform(multipart("/api/v1/images")
                        .file(validPngFile())
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.imageId").isNotEmpty())
                .andExpect(jsonPath("$.data.imageURL").value(
                        org.hamcrest.Matchers.startsWith("https://fake-storage.test/gallery/images/")))
                .andReturn();

        Image persistedImage = imageRepository.findAll().stream()
                .findFirst()
                .orElseThrow();

        assertTrue(result.getResponse().getContentAsString()
                .contains(persistedImage.getId().toString()));
        assertEquals(
                "https://fake-storage.test/gallery/images/" + persistedImage.getId(),
                persistedImage.getImageUrl());
        assertNotNull(persistedImage.getCreatedAt());

        org.mockito.ArgumentCaptor<UUID> imageIdCaptor =
                org.mockito.ArgumentCaptor.forClass(UUID.class);
        org.mockito.ArgumentCaptor<MultipartFile> fileCaptor =
                org.mockito.ArgumentCaptor.forClass(MultipartFile.class);
        verify(imageStorage).upload(imageIdCaptor.capture(), fileCaptor.capture());

        assertEquals(persistedImage.getId(), imageIdCaptor.getValue());
        assertArrayEquals(PNG_BYTES, fileCaptor.getValue().getBytes());
    }

    /**
     * UC04 phải trả lỗi validation khi multipart không có part bắt buộc `file`.
     */
    // Kiểm tra request thiếu field không được chuyển thành một lần upload rỗng.
    @Test
    void uploadWithoutMultipartFileReturnsValidationErrorWithoutSideEffects() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(multipart("/api/v1/images")
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC04 phải trả lỗi 400 khi part `file` có tồn tại nhưng không có bytes.
     */
    // Kiểm tra file rỗng bị chặn bởi validator thật trước Storage.
    @Test
    void uploadWithEmptyMultipartFileReturnsRequiredErrorWithoutSideEffects() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/v1/images")
                        .file(emptyFile)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("IMAGE_FILE_REQUIRED"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC04 phải kiểm tra actual bytes thay vì tin extension và Content-Type của client.
     */
    // Kiểm tra file giả mạo không thể đi tới Storage dù metadata nhìn hợp lệ.
    @Test
    void uploadWithMismatchedImageBytesReturnsUnsupportedMediaTypeWithoutSideEffects()
            throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "fake.jpg", MediaType.IMAGE_JPEG_VALUE, "not-an-image".getBytes());

        mockMvc.perform(multipart("/api/v1/images")
                        .file(fakeImage)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC04 phải từ chối file vượt quá giới hạn 15 MB trước khi gọi Storage.
     */
    // Kiểm tra boundary phía trên và bảo đảm không có row mồ côi khi request bị từ chối.
    @Test
    void uploadOverMaximumSizeReturnsPayloadTooLargeWithoutSideEffects() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();
        byte[] oversizedPng = new byte[(int) org.example.authentication_cookiesession.service.ImageFileValidator.MAX_FILE_SIZE_BYTES + 1];
        System.arraycopy(PNG_BYTES, 0, oversizedPng, 0, PNG_BYTES.length);
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file", "oversized.png", MediaType.IMAGE_PNG_VALUE, oversizedPng);

        mockMvc.perform(multipart("/api/v1/images")
                        .file(oversizedFile)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.code").value("IMAGE_FILE_TOO_LARGE"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC04 phải trả lỗi 500 an toàn khi Storage upload thất bại.
     */
    // Kiểm tra lỗi dependency không tạo database record và không lộ chi tiết nội bộ.
    @Test
    void uploadWhenStorageFailsReturnsSafeServerErrorWithoutDatabaseRecord() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();
        when(imageStorage.upload(any(UUID.class), any(MultipartFile.class)))
                .thenThrow(new ImageStorageException(
                        "storage unavailable", new IllegalStateException("upstream down")));

        mockMvc.perform(multipart("/api/v1/images")
                        .file(validPngFile())
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("The server could not process the request"));

        verify(imageStorage).upload(any(UUID.class), any(MultipartFile.class));
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC01 phải cho visitor đọc record PostgreSQL mà không cần session admin.
     */
    // Kiểm tra public listing qua controller, service và repository thật.
    @Test
    void publicGalleryListsPersistedImageWithoutAuthentication() throws Exception {
        UUID imageId = UUID.randomUUID();
        String imageUrl = "https://fake-storage.test/gallery/images/" + imageId;
        imageRepository.insertUploadedImage(imageId, imageUrl);

        mockMvc.perform(get("/api/v1/images")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("IMAGES_RETRIEVING_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.items[0].imageId").value(imageId.toString()))
                .andExpect(jsonPath("$.data.items[0].imageURL").value(imageUrl))
                .andExpect(jsonPath("$.data.pagination.limit").value(10));
    }

    /**
     * UC05 phải xóa Storage object theo path server-generated rồi xóa row PostgreSQL.
     */
    // Kiểm tra delete qua session, CSRF, mock Storage và database thật.
    @Test
    void deleteRemovesStorageObjectAndDatabaseRecord() throws Exception {
        UUID imageId = UUID.randomUUID();
        imageRepository.insertUploadedImage(
                imageId,
                "https://fake-storage.test/gallery/images/" + imageId);

        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("IMAGE_DELETING_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.data.imageId").value(imageId.toString()));

        assertTrue(imageRepository.findById(imageId).isEmpty());
        verify(imageStorage).delete("images/" + imageId);
    }

    /**
     * UC05 phải trả 401 nếu request không có session admin hợp lệ.
     */
    // Kiểm tra delete không thể truy cập khi chưa đăng nhập.
    @Test
    void deleteWithoutAdminSessionIsRejected() throws Exception {
        Cookie csrfCookie = requestCsrfToken();
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC05 phải từ chối token CSRF không khớp và giữ nguyên image record.
     */
    // Kiểm tra delete không thể bypass CSRF để tác động dữ liệu thật.
    @Test
    void deleteWithInvalidCsrfTokenIsRejectedAndImageRemains() throws Exception {
        UUID imageId = UUID.randomUUID();
        imageRepository.insertUploadedImage(
                imageId,
                "https://fake-storage.test/gallery/images/" + imageId);
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, "invalid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_FORBIDDEN"));

        assertTrue(imageRepository.findById(imageId).isPresent());
        verifyNoInteractions(imageStorage);
    }

    /**
     * UC05 phải từ chối request thiếu CSRF header và giữ nguyên image record.
     */
    // Kiểm tra delete không thể bypass bằng cách chỉ gửi session cookie.
    @Test
    void deleteWithoutCsrfHeaderIsRejectedAndImageRemains() throws Exception {
        UUID imageId = UUID.randomUUID();
        imageRepository.insertUploadedImage(
                imageId,
                "https://fake-storage.test/gallery/images/" + imageId);
        Cookie sessionCookie = loginAsAdmin();

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(sessionCookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_FORBIDDEN"));

        assertTrue(imageRepository.findById(imageId).isPresent());
        verifyNoInteractions(imageStorage);
    }

    /**
     * UC05 phải trả 404 khi imageId không tồn tại và không gọi Storage.
     */
    // Kiểm tra lookup database xảy ra trước thao tác xóa external object.
    @Test
    void deleteMissingImageReturnsNotFoundWithoutStorageCall() throws Exception {
        UUID imageId = UUID.randomUUID();
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findById(imageId).isEmpty());
    }

    /**
     * UC05 phải trả 400 nếu path variable không parse được thành UUID.
     */
    // Kiểm tra input path malformed không bị che thành lỗi server nội bộ.
    @Test
    void deleteWithMalformedImageIdReturnsValidationErrorWithoutStorageCall() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/images/not-a-uuid")
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(imageStorage);
        assertTrue(imageRepository.findAll().isEmpty());
    }

    /**
     * UC05 phải giữ database record nếu Storage không xóa được object.
     */
    // Kiểm tra thứ tự Storage-first không biến lỗi external thành mất row database.
    @Test
    void deleteWhenStorageFailsReturnsSafeServerErrorAndKeepsDatabaseRecord() throws Exception {
        UUID imageId = UUID.randomUUID();
        imageRepository.insertUploadedImage(
                imageId,
                "https://fake-storage.test/gallery/images/" + imageId);
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();
        doThrow(new ImageStorageException(
                "storage unavailable", new IllegalStateException("upstream down")))
                .when(imageStorage)
                .delete("images/" + imageId);

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("IMAGE_DELETION_FAILED"))
                .andExpect(jsonPath("$.message").value("The image could not be deleted"));

        assertTrue(imageRepository.findById(imageId).isPresent());
        verify(imageStorage).delete("images/" + imageId);
    }

    /**
     * UC05 phải trả 404 cho lần xóa lặp lại sau khi lần đầu đã thành công.
     */
    // Kiểm tra delete không lặp lại thao tác Storage khi database row đã biến mất.
    @Test
    void repeatedDeleteAfterSuccessReturnsNotFoundWithoutSecondStorageCall() throws Exception {
        UUID imageId = UUID.randomUUID();
        imageRepository.insertUploadedImage(
                imageId,
                "https://fake-storage.test/gallery/images/" + imageId);
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMAGE_NOT_FOUND"));

        verify(imageStorage).delete("images/" + imageId);
        assertTrue(imageRepository.findById(imageId).isEmpty());
    }

    /**
     * UC06 phải invalidate Redis session; cookie cũ không được dùng lại cho UC03.
     */
    // Kiểm tra logout và hiệu lực session sau khi invalidation hoàn tất.
    @Test
    void logoutInvalidatesRedisSessionAndOldCookieCannotAccessCurrentSession() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        MvcResult logoutResult = mockMvc.perform(delete("/api/v1/auth/admin/sessions")
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("ADMIN_LOGOUT_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.authenticated").value(false))
                .andReturn();

        String setCookie = logoutResult.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains(SESSION_COOKIE_NAME + "="));
        assertTrue(setCookie.contains("Max-Age=0"));

        mockMvc.perform(get("/api/v1/auth/admin/sessions")
                        .cookie(sessionCookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"));
    }

    /**
     * UC06 phải yêu cầu session admin trước khi logout.
     */
    // Kiểm tra logout không thể được dùng để tạo hoặc tác động session khác.
    @Test
    void logoutWithoutSessionIsRejected() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/auth/admin/sessions")
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"));
    }

    /**
     * UC06 phải bị CSRF filter chặn và giữ nguyên session khi header bị thiếu.
     */
    // Kiểm tra request logout không được invalidate session trước khi CSRF hợp lệ.
    @Test
    void logoutWithoutCsrfHeaderIsRejectedAndSessionRemainsValid() throws Exception {
        Cookie sessionCookie = loginAsAdmin();

        mockMvc.perform(delete("/api/v1/auth/admin/sessions")
                        .cookie(sessionCookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_FORBIDDEN"));

        mockMvc.perform(get("/api/v1/auth/admin/sessions")
                        .cookie(sessionCookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    /**
     * UC06 phải bị CSRF filter chặn khi header không khớp cookie.
     */
    // Kiểm tra token giả không thể làm mất session hợp lệ.
    @Test
    void logoutWithInvalidCsrfTokenIsRejectedAndSessionRemainsValid() throws Exception {
        Cookie sessionCookie = loginAsAdmin();
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/auth/admin/sessions")
                        .cookie(sessionCookie, csrfCookie)
                        .header(CSRF_HEADER_NAME, "invalid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ADMIN_ACCESS_FORBIDDEN"));

        mockMvc.perform(get("/api/v1/auth/admin/sessions")
                        .cookie(sessionCookie)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    /**
     * UC06 phải từ chối cookie session giả dù CSRF token hợp lệ.
     */
    // Kiểm tra logout không chấp nhận session ID không tồn tại trong Redis.
    @Test
    void logoutWithUnknownSessionCookieIsRejected() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        mockMvc.perform(delete("/api/v1/auth/admin/sessions")
                        .cookie(new Cookie(SESSION_COOKIE_NAME, "unknown-session-id"), csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"));
    }

    /**
     * Thực hiện login theo đúng JSON contract và trả cookie session từ response.
     */
    private Cookie loginAsAdmin() throws Exception {
        Cookie csrfCookie = requestCsrfToken();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrfCookie)
                        .header(CSRF_HEADER_NAME, csrfCookie.getValue())
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("ADMIN_LOGIN_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie(SESSION_COOKIE_NAME);
        assertNotNull(sessionCookie);
        assertFalse(sessionCookie.getValue().isBlank());
        return sessionCookie;
    }

    /**
     * Lấy CSRF cookie từ endpoint công khai để dùng cho request state-changing.
     */
    private Cookie requestCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie);
        assertFalse(csrfCookie.getValue().isBlank());
        return csrfCookie;
    }

    /**
     * Tạo UUID deterministic để assertion thứ tự pagination không phụ thuộc random.
     */
    private UUID imageId(int number) {
        return UUID.fromString(String.format(
                "00000000-0000-0000-0000-%012d", number));
    }

    /**
     * Chèn image fixture với created_at xác định để kiểm tra keyset boundary.
     */
    private void insertImageWithCreatedAt(int number, OffsetDateTime createdAt) {
        UUID imageId = imageId(number);
        jdbcTemplate.update(
                "INSERT INTO images (id, image_url, created_at) VALUES (?, ?, ?)",
                imageId,
                "https://fake-storage.test/gallery/images/" + imageId,
                createdAt);
    }

    /**
     * Tạo multipart PNG tối thiểu có magic bytes hợp lệ theo ImageFileValidator.
     */
    private org.springframework.mock.web.MockMultipartFile validPngFile() {
        return new org.springframework.mock.web.MockMultipartFile(
                "file",
                "integration-image.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES);
    }
}
