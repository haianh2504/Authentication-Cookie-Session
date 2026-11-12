package org.example.authentication_cookiesession.dto.response;

import java.util.UUID;

/**
 * DTO trả về sau khi file đã được lưu thành công ở Storage và PostgreSQL.
 *
 * @param imageId ID được backend tạo trước khi upload
 * @param imageURL public URL dùng để frontend hiển thị ảnh
 */
public record ImageUploadResponse(
        UUID imageId,
        String imageURL
) {
}
