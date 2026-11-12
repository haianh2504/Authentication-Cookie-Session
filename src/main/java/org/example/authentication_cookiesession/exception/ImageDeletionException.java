package org.example.authentication_cookiesession.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi an toàn dành cho use case xóa ảnh.
 */
public class ImageDeletionException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String clientMessage;

    public ImageDeletionException(
            HttpStatus status,
            String code,
            String clientMessage) {
        super(clientMessage);
        this.status = status;
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public ImageDeletionException(
            HttpStatus status,
            String code,
            String clientMessage,
            Throwable cause) {
        super(clientMessage, cause);
        this.status = status;
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public static ImageDeletionException unauthorized() {
        return new ImageDeletionException(
                HttpStatus.UNAUTHORIZED,
                "ADMIN_SESSION_UNAUTHORIZED",
                "Your session is invalid or has expired. Please sign in again.");
    }

    public static ImageDeletionException notFound() {
        return new ImageDeletionException(
                HttpStatus.NOT_FOUND,
                "IMAGE_NOT_FOUND",
                "The image could not be found");
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
