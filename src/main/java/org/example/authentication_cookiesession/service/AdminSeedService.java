package org.example.authentication_cookiesession.service;

import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.config.AdminSeedProperties;
import org.example.authentication_cookiesession.entity.Admin;
import org.example.authentication_cookiesession.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Tạo admin ban đầu một cách idempotent và chỉ lưu password hash vào database.
 */
@Service
@RequiredArgsConstructor
public class AdminSeedService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminSeedProperties;

    /**
     * Kiểm tra cấu hình, bỏ qua nếu admin đã tồn tại và tạo admin mới nếu cần.
     *
     * @throws IllegalStateException khi seed được bật nhưng thiếu cấu hình bắt buộc
     */
    @Transactional
    public void seedIfNecessary() {
        String email = normalizedEmail();
        String initialPassword = adminSeedProperties.initialPassword();

        validateConfiguration(email, initialPassword);

        if (adminRepository.existsById(email)) {
            return;
        }

        String passwordHash = passwordEncoder.encode(initialPassword);
        adminRepository.save(Admin.create(email, passwordHash));
    }

    /**
     * Chuẩn hóa email trước khi dùng làm khóa chính và tra cứu tài khoản.
     *
     * @return email đã bỏ khoảng trắng và chuyển về lowercase
     */
    private String normalizedEmail() {
        return adminSeedProperties.email() == null
                ? null
                : adminSeedProperties.email().trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Kiểm tra cấu hình mà không đưa giá trị secret vào exception message.
     */
    private void validateConfiguration(String email, String initialPassword) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("ADMIN_EMAIL must be configured when admin seeding is enabled");
        }

        if (!StringUtils.hasText(initialPassword)) {
            throw new IllegalStateException(
                    "ADMIN_INITIAL_PASSWORD must be configured when admin seeding is enabled");
        }
    }
}
