package org.example.authentication_cookiesession.repository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm tra các boundary của adapter thao tác session hiện tại. */
@ExtendWith(MockitoExtension.class)
class AdminSessionRepositoryTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    /**
     * Không có session hiện tại phải trả false và không được tạo session mới.
     */
    // Kiểm tra logout/session lookup không tạo session ngầm.
    @Test
    void returnsFalseWhenCurrentSessionDoesNotExist() {
        when(request.getSession(false)).thenReturn(null);

        assertFalse(new AdminSessionRepository().invalidateCurrentSession(request));

        verify(request).getSession(false);
    }

    /**
     * Có session hiện tại phải invalidate đúng object và trả true.
     */
    // Kiểm tra adapter thực sự xóa session backend được Spring Session bọc.
    @Test
    void invalidatesExistingSessionAndReturnsTrue() {
        when(request.getSession(false)).thenReturn(session);

        assertTrue(new AdminSessionRepository().invalidateCurrentSession(request));

        verify(request).getSession(false);
        verify(session).invalidate();
    }
}
