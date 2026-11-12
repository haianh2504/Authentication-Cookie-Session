package org.example.authentication_cookiesession.storage;

import org.example.authentication_cookiesession.config.SupabaseStorageProperties;
import org.example.authentication_cookiesession.exception.ImageStorageException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.UUID;

/**
 * Adapter gọi Supabase Storage REST API.
 *
 * <p>Supabase Storage nhận raw binary ở endpoint object. Backend gửi file trực
 * tiếp bằng resource của MultipartFile, không tự đổi tên theo filename client.
 * Secret key chỉ nằm ở header server-to-server và không bao giờ đi vào
 * response.</p>
 */
@Component
public class SupabaseImageStorage implements ImageStorage {

    private static final String API_KEY_HEADER = "apikey";
    private static final String UPSERT_HEADER = "x-upsert";

    private final RestClient restClient;
    private final SupabaseStorageProperties properties;

    public SupabaseImageStorage(
            RestClient restClient,
            SupabaseStorageProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Upload raw image bytes vào bucket public gallery tại images/{imageId}.
     */
    @Override
    public StoredImage upload(UUID imageId, MultipartFile file) {
        ensureConfigured();
        String storagePath = "images/" + imageId;
        String contentType = file.getContentType();

        try {
            restClient.post()
                    .uri(objectUri(storagePath))
                    .headers(this::addAuthenticationHeaders)
                    .header(UPSERT_HEADER, "false")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(file.getSize())
                    .body(file.getResource())
                    .retrieve()
                    .toBodilessEntity();

            return new StoredImage(storagePath, publicUrl(storagePath));
        } catch (RuntimeException exception) {
            throw new ImageStorageException(
                    "Supabase Storage upload failed for image " + imageId,
                    exception);
        }
    }

    /**
     * Xóa object bằng Storage API, không thao tác trực tiếp vào storage.objects.
     */
    @Override
    public void delete(String storagePath) {
        ensureConfigured();
        try {
            restClient.delete()
                    .uri(objectUri(storagePath))
                    .headers(this::addAuthenticationHeaders)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            throw new ImageStorageException(
                    "Supabase Storage cleanup failed for path " + storagePath,
                    exception);
        }
    }

    /**
     * Tạo URI object bằng từng path segment để không cho filename client chèn
     * thêm path hoặc query parameter vào request Storage.
     */
    private URI objectUri(String storagePath) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.baseUrl())
                .pathSegment("storage", "v1", "object", properties.bucket());

        for (String pathSegment : storagePath.split("/")) {
            builder.pathSegment(pathSegment);
        }
        return builder.build().toUri();
    }

    /**
     * Tạo public URL tương ứng với object đã upload.
     */
    private String publicUrl(String storagePath) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.baseUrl())
                .pathSegment("storage", "v1", "object", "public", properties.bucket());

        for (String pathSegment : storagePath.split("/")) {
            builder.pathSegment(pathSegment);
        }
        return builder.build().toUriString();
    }

    /**
     * Secret key mới của Supabase được gửi qua header apikey.
     */
    private void addAuthenticationHeaders(HttpHeaders headers) {
        headers.set(API_KEY_HEADER, properties.apiKey());
    }

    /**
     * Cho phép application context khởi động khi chưa cấu hình upload, nhưng
     * chặn rõ ràng việc gọi Storage thiếu thông tin bắt buộc.
     */
    private void ensureConfigured() {
        if (!StringUtils.hasText(properties.baseUrl())
                || !StringUtils.hasText(properties.apiKey())
                || !StringUtils.hasText(properties.bucket())) {
            throw new ImageStorageException(
                    "Supabase Storage configuration is incomplete",
                    new IllegalStateException("SUPABASE_URL, SUPABASE_SECRET_KEY and bucket are required"));
        }
    }
}
