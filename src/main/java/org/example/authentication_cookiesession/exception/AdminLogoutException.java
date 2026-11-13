package org.example.authentication_cookiesession.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi an toàn dành cho use case logout admin.
 */
public class AdminLogoutException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String clientMessage;

    public AdminLogoutException(
            HttpStatus status,
            String code,
            String clientMessage) {
        super(clientMessage);
        this.status = status;
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public AdminLogoutException(
            HttpStatus status,
            String code,
            String clientMessage,
            Throwable cause) {
        super(clientMessage, cause);
        this.status = status;
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public static AdminLogoutException unauthorized() {
        return new AdminLogoutException(
                HttpStatus.UNAUTHORIZED,
                "ADMIN_SESSION_UNAUTHORIZED",
                "Your session is invalid or has expired. Please sign in again.");
    }

    public static AdminLogoutException failed(Throwable cause) {
        return new AdminLogoutException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ADMIN_LOGOUT_FAILED",
                "The server could not invalidate the session",
                cause);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getClientMessage() {
        return clientMessage;
    }
}
