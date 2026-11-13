package org.example.authentication_cookiesession.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Cấu hình tên token, thuộc tính cookie và độ dài token CSRF. */
@Validated
@ConfigurationProperties(prefix = "app.security.csrf")
public record CsrfSecurityProperties(
        @NotBlank String cookieName,
        @NotBlank String headerName,
        @NotBlank String parameterName,
        @NotBlank String cookiePath,
        @NotBlank
        @Pattern(regexp = "Lax|Strict|None")
        String sameSite,
        String cookieDomain,
        boolean cookieSecure,
        @Min(32) int tokenBytes
) {
}
