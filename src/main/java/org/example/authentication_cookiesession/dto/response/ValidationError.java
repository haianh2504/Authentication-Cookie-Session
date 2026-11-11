package org.example.authentication_cookiesession.dto.response;

/**
 * Mô tả một lỗi validation gắn với một field trong request.
 *
 * @param field tên field không hợp lệ
 * @param message thông báo validation an toàn dành cho client
 */
public record ValidationError(String field, String message) {
}
