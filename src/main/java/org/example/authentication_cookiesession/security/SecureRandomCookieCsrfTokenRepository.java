package org.example.authentication_cookiesession.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authentication_cookiesession.config.CsrfSecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/** Tạo, lưu và đọc CSRF token theo mô hình double-submit cookie. */
public final class SecureRandomCookieCsrfTokenRepository implements CsrfTokenRepository {

    private final CsrfSecurityProperties properties;
    private final SecureRandom secureRandom;

    /** Tạo repository với bộ sinh số ngẫu nhiên bảo mật mặc định của Java. */
    public SecureRandomCookieCsrfTokenRepository(CsrfSecurityProperties properties) {
        this(properties, new SecureRandom());
    }

    /** Tạo repository với bộ sinh ngẫu nhiên được truyền vào để dễ kiểm thử. */
    SecureRandomCookieCsrfTokenRepository(
            CsrfSecurityProperties properties,
            SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    /** Sinh token ngẫu nhiên và mã hóa thành chuỗi Base64 URL-safe. */
    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        byte[] randomBytes = new byte[properties.tokenBytes()];
        secureRandom.nextBytes(randomBytes);

        String tokenValue = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return new DefaultCsrfToken(
                properties.headerName(),
                properties.parameterName(),
                tokenValue);
    }

    /** Ghi token vào cookie frontend có thể đọc hoặc xóa cookie khi token null. */
    @Override
    public void saveToken(
            CsrfToken token,
            HttpServletRequest request,
            HttpServletResponse response) {
        String tokenValue = token == null ? "" : token.getToken();
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie
                .from(properties.cookieName(), tokenValue)
                .httpOnly(false)
                .secure(properties.cookieSecure())
                .path(properties.cookiePath())
                .sameSite(properties.sameSite())
                .maxAge(token == null ? Duration.ZERO : Duration.ofSeconds(-1));

        if (StringUtils.hasText(properties.cookieDomain())) {
            cookieBuilder.domain(properties.cookieDomain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    /** Đọc token hiện tại từ cookie CSRF của request. */
    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.cookieName());
        if (cookie == null || !StringUtils.hasText(cookie.getValue())) {
            return null;
        }

        return new DefaultCsrfToken(
                properties.headerName(),
                properties.parameterName(),
                cookie.getValue());
    }
}
