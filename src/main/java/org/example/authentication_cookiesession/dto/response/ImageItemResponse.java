package org.example.authentication_cookiesession.dto.response;

import java.util.UUID;

/**
 * Đại diện cho một ảnh trong danh sách gallery công khai.
 *
 * @param imageId định danh duy nhất của ảnh trong bảng {@code images}
 * @param imageURL public URL để frontend tải và hiển thị ảnh
 */
public record ImageItemResponse(
        UUID imageId,
        String imageURL
) {
}
