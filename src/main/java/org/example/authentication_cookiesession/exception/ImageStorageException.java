package org.example.authentication_cookiesession.exception;

/**
 * Lỗi nội bộ khi gọi Supabase Storage.
 *
 * <p>Lỗi này không được trả nguyên văn cho client. Nó chỉ giúp service phân
 * biệt lỗi Storage với lỗi validation và ghi log an toàn khi cần.</p>
 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
