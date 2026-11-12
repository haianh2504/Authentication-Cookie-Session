package org.example.authentication_cookiesession.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi an toàn dành cho use case upload ảnh.
 *
 * <p>Exception này giữ status, code và message public tách khỏi nguyên nhân kỹ
 * thuật. Vì vậy controller advice có thể trả lỗi đúng contract mà không làm lộ
 * stack trace, service-role key hoặc chi tiết Supabase cho client.</p>
 */
public class ImageUploadException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String clientMessage;

    public ImageUploadException(
            HttpStatus status,
            String code,
            String clientMessage) {
        super(clientMessage);
        this.status = status;
        this.code = code;
        this.clientMessage = clientMessage;
    }

    public ImageUploadException(
            HttpStatus status,
            String code,
            String clientMessage,
            Throwable cause) {
        super(clientMessage, cause);
        this.status = status;
        this.code = code;
        this.clientMessage = clientMessage;
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
