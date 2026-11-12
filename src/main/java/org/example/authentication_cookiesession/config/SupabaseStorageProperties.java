package org.example.authentication_cookiesession.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Các thông tin kết nối tới Supabase Storage.
 *
 * <p>Secret key chỉ được dùng ở backend và phải được truyền qua biến môi
 * trường. Không đưa key này vào DTO, response hoặc log.</p>
 */
@ConfigurationProperties(prefix = "app.supabase.storage")
public record SupabaseStorageProperties(
        String baseUrl,
        String apiKey,
        String bucket
) {
}
