package org.example.authentication_cookiesession.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cấu hình bộ mã hóa mật khẩu dùng chung cho toàn bộ ứng dụng.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Tạo BCrypt encoder để chỉ lưu mật khẩu dưới dạng một chiều trong database.
     *
     * @return bộ mã hóa và kiểm tra mật khẩu BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
