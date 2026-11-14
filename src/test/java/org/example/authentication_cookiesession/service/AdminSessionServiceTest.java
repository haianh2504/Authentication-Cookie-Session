package org.example.authentication_cookiesession.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authentication_cookiesession.dto.request.AdminLoginRequest;
import org.example.authentication_cookiesession.dto.response.AuthenticationStatusResponse;
import org.example.authentication_cookiesession.exception.AdminLogoutException;
import org.example.authentication_cookiesession.repository.AdminSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {

    @Mock
    private AdminSessionRepository adminSessionRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    private AdminSessionService service;

    @BeforeEach
    void setUp() {
        service = new AdminSessionService(
                authenticationManager,
                securityContextRepository,
                adminSessionRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidatesCurrentSession() {
        when(adminSessionRepository.invalidateCurrentSession(request)).thenReturn(true);

        service.logout(request);

        verify(adminSessionRepository).invalidateCurrentSession(request);
    }

    /**
     * UC03 phải trả trạng thái chưa xác thực khi service nhận authentication null.
     */
    // Kiểm tra nhánh null an toàn của logic đọc trạng thái session.
    @Test
    void reportsUnauthenticatedWhenAuthenticationIsNull() {
        assertFalse(service.getCurrentSession(null).authenticated());
    }

    /**
     * UC03 phải phản ánh đúng cờ authenticated từ Spring Security.
     */
    // Kiểm tra cả authentication hợp lệ và authentication chưa được xác thực.
    @Test
    void reportsAuthenticationStateWithoutExposingSensitiveData() {
        when(authentication.isAuthenticated()).thenReturn(false, true);

        assertFalse(service.getCurrentSession(authentication).authenticated());
        AuthenticationStatusResponse response =
                service.getCurrentSession(authentication);
        assertTrue(response.authenticated());
    }

    /**
     * Login phải xác thực, đổi session ID và lưu SecurityContext vào session store.
     */
    @Test
    void loginChangesSessionIdAndPersistsAuthenticatedContext() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        service.login(
                new AdminLoginRequest("admin@example.com", "password"),
                request,
                response);

        verify(authenticationManager).authenticate(any());
        verify(request).getSession(true);
        verify(request).changeSessionId();
        verify(securityContextRepository).saveContext(any(), same(request), same(response));
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Authentication failure không được tạo hoặc xoay session.
     */
    @Test
    void failedLoginDoesNotCreateOrRotateSession() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException(
                        "invalid credentials"));

        assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> service.login(
                        new AdminLoginRequest("admin@example.com", "wrong-password"),
                        request,
                        response));

        verify(request, never()).getSession(true);
        verify(request, never()).changeSessionId();
        verify(securityContextRepository, never()).saveContext(any(), any(), any());
    }

    @Test
    void rejectsLogoutWhenCurrentSessionDoesNotExist() {
        when(adminSessionRepository.invalidateCurrentSession(request)).thenReturn(false);

        AdminLogoutException exception = assertThrows(
                AdminLogoutException.class,
                () -> service.logout(request));

        assertEquals(401, exception.getStatus().value());
        assertEquals("ADMIN_SESSION_UNAUTHORIZED", exception.getCode());
    }

    /**
     * UC06 phải chuyển lỗi Redis/session store thành lỗi 500 an toàn.
     */
    // Kiểm tra không trả logout success khi backend không invalidate được session.
    @Test
    void wrapsLogoutRepositoryFailureAndClearsSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(adminSessionRepository.invalidateCurrentSession(request))
                .thenThrow(new IllegalStateException("redis unavailable"));

        AdminLogoutException exception = assertThrows(
                AdminLogoutException.class,
                () -> service.logout(request));

        assertEquals(500, exception.getStatus().value());
        assertEquals("ADMIN_LOGOUT_FAILED", exception.getCode());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * UC06 phải xóa SecurityContext kể cả khi invalidate thành công.
     */
    // Kiểm tra authentication không còn tồn tại trên thread sau logout.
    @Test
    void clearsSecurityContextAfterSuccessfulLogout() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(adminSessionRepository.invalidateCurrentSession(request)).thenReturn(true);

        service.logout(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
