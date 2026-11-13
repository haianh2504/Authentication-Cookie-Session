package org.example.authentication_cookiesession.security;

import jakarta.servlet.http.Cookie;
import org.example.authentication_cookiesession.config.CsrfSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm tra sinh token và vòng đời cookie CSRF. */
class SecureRandomCookieCsrfTokenRepositoryTest {

    private static final CsrfSecurityProperties PROPERTIES = new CsrfSecurityProperties(
            "XSRF-TOKEN",
            "X-XSRF-TOKEN",
            "_csrf",
            "/",
            "Lax",
            "",
            false,
            32);

    /** Đảm bảo token đủ dài và chỉ chứa ký tự Base64 URL-safe. */
    @Test
    void generatesIndependentBase64UrlTokenWithConfiguredLength() {
        SecureRandomCookieCsrfTokenRepository repository = repository();

        CsrfToken first = repository.generateToken(new MockHttpServletRequest());
        CsrfToken second = repository.generateToken(new MockHttpServletRequest());

        assertEquals("X-XSRF-TOKEN", first.getHeaderName());
        assertEquals("_csrf", first.getParameterName());
        assertEquals(43, first.getToken().length());
        assertTrue(first.getToken().matches("[A-Za-z0-9_-]+"));
        assertNotEquals(first.getToken(), second.getToken());
    }

    /** Đảm bảo cookie CSRF đọc được bởi frontend và có SameSite phù hợp. */
    @Test
    void savesFrontendReadableLaxCookie() {
        SecureRandomCookieCsrfTokenRepository repository = repository();
        CsrfToken token = repository.generateToken(new MockHttpServletRequest());
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveToken(token, new MockHttpServletRequest(), response);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("XSRF-TOKEN=" + token.getToken()));
        assertTrue(setCookie.contains("Path=/"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertFalse(setCookie.contains("HttpOnly"));
        assertFalse(setCookie.contains("Secure"));
    }

    /** Đảm bảo repository đọc đúng token từ cookie request. */
    @Test
    void loadsTokenFromConfiguredCookie() {
        SecureRandomCookieCsrfTokenRepository repository = repository();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("XSRF-TOKEN", "token-from-cookie"));

        CsrfToken token = repository.loadToken(request);

        assertNotNull(token);
        assertEquals("token-from-cookie", token.getToken());
        assertEquals("X-XSRF-TOKEN", token.getHeaderName());
    }

    /** Đảm bảo token null làm cookie hết hạn. */
    @Test
    void expiresCookieWhenTokenIsDeleted() {
        SecureRandomCookieCsrfTokenRepository repository = repository();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveToken(null, new MockHttpServletRequest(), response);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("XSRF-TOKEN=;"));
        assertTrue(setCookie.contains("Max-Age=0"));
        assertNull(repository.loadToken(new MockHttpServletRequest()));
    }

    /** Tạo repository dùng chung cho các test. */
    private SecureRandomCookieCsrfTokenRepository repository() {
        return new SecureRandomCookieCsrfTokenRepository(PROPERTIES, new SecureRandom());
    }
}
