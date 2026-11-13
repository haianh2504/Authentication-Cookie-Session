package org.example.authentication_cookiesession.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.authentication_cookiesession.exception.AdminLogoutException;
import org.example.authentication_cookiesession.repository.AdminSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {

    @Mock
    private AdminSessionRepository adminSessionRepository;

    @Mock
    private HttpServletRequest request;

    private AdminSessionService service;

    @BeforeEach
    void setUp() {
        service = new AdminSessionService(null, null, adminSessionRepository);
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

    @Test
    void rejectsLogoutWhenCurrentSessionDoesNotExist() {
        when(adminSessionRepository.invalidateCurrentSession(request)).thenReturn(false);

        AdminLogoutException exception = assertThrows(
                AdminLogoutException.class,
                () -> service.logout(request));

        assertEquals(401, exception.getStatus().value());
        assertEquals("ADMIN_SESSION_UNAUTHORIZED", exception.getCode());
    }
}
