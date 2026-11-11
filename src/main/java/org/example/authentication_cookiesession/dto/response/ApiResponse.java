package org.example.authentication_cookiesession.dto.response;

import java.time.OffsetDateTime;

/**
 * Envelope dùng chung cho mọi response thành công và lỗi của API.
 *
 * @param status HTTP status của response
 * @param code mã kết quả ổn định để client xử lý
 * @param message thông báo an toàn dành cho client
 * @param timestamp thời điểm tạo response theo chuẩn ISO 8601
 * @param data dữ liệu riêng của từng endpoint, có thể là object hoặc collection
 * @param <T> kiểu dữ liệu bên trong data
 */
public record ApiResponse<T>(
        int status,
        String code,
        String message,
        OffsetDateTime timestamp,
        T data
) {

    /**
     * Tạo response envelope với timestamp hiện tại của server.
     *
     * @param status HTTP status của response
     * @param code mã kết quả ổn định
     * @param message thông báo an toàn dành cho client
     * @param data dữ liệu riêng của endpoint
     * @param <T> kiểu dữ liệu bên trong data
     * @return response envelope hoàn chỉnh
     */
    public static <T> ApiResponse<T> of(int status, String code, String message, T data) {
        return new ApiResponse<>(status, code, message, OffsetDateTime.now(), data);
    }
}
