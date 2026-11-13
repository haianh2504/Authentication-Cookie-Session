package org.example.authentication_cookiesession.repository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Repository;

/**
 * Cổng thao tác với session hiện tại thông qua Spring Session.
 *
 * <p>Vì request được Spring Session bọc lại, gọi {@link HttpSession#invalidate()}
 * sẽ xóa session backend trong Redis. SessionRepositoryFilter cũng sẽ gửi cookie
 * hết hạn khi response được commit.</p>
 */
@Repository
public class AdminSessionRepository {

    /**
     * Xóa session hiện tại nếu request còn session hợp lệ.
     *
     * @param request HTTP request hiện tại
     * @return {@code true} nếu đã invalidate một session, ngược lại {@code false}
     */
    public boolean invalidateCurrentSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        session.invalidate();
        return true;
    }
}
