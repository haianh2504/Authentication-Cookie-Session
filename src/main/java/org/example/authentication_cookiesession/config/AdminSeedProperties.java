package org.example.authentication_cookiesession.config;

import jakarta.validation.constraints.Email;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bind cấu hình dùng để provision admin ban đầu từ environment variables.
 *
 * @param enabled cho phép chạy seed khi khởi động ứng dụng
 * @param email email admin cần tạo nếu chưa tồn tại
 * @param initialPassword plaintext password chỉ dùng tạm thời để tạo BCrypt hash
 */
@ConfigurationProperties(prefix = "app.admin.seed")
/** Bind nhóm cấu hình có prefix:    app.admin.seed.*    vào record Java.
 **/
@Validated // Bật validation cho configuration properties.
public record AdminSeedProperties(
        boolean enabled,
        @Email(message = "ADMIN_EMAIL must be a valid email address")
        String email,
        String initialPassword
) {
}
