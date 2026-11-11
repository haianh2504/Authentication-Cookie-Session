package org.example.authentication_cookiesession.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dữ liệu đăng nhập của administrator theo use case UC02.
 *
 * @param email email administrator dùng để xác thực
 * @param password mật khẩu dạng plaintext chỉ tồn tại trong request và không được ghi log
 */
public record AdminLoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password
) {

    /**
     * Che mật khẩu nếu request bị ghi log ngoài ý muốn.
     *
     * @return chuỗi mô tả request không chứa plaintext password
     */
    @Override
    public String toString() {
        return "AdminLoginRequest[email='" + email + "', password='[REDACTED]']";
    }
}
