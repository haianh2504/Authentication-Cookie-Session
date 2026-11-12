package org.example.authentication_cookiesession.dto.response;

import java.util.UUID;

/**
 * DTO trả về sau khi Storage object và database record đã được xóa.
 *
 * @param deleted cho biết thao tác xóa đã hoàn tất
 * @param imageId UUID của ảnh đã xóa
 */
public record ImageDeleteResponse(
        boolean deleted,
        UUID imageId
) {
}
