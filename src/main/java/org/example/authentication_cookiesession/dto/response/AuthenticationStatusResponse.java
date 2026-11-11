package org.example.authentication_cookiesession.dto.response;

/**
 * Dữ liệu trạng thái xác thực dùng chung cho login và kiểm tra session hiện tại.
 *
 * @param authenticated cho biết request hiện tại đã được xác thực hay chưa
 */
public record AuthenticationStatusResponse(boolean authenticated) {
}
