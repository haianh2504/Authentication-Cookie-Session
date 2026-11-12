package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.dto.response.ImageDeleteResponse;

import java.util.UUID;

/**
 * Nghiệp vụ chính của use case Delete an image.
 */
public interface ImageDeletionService {

    /**
     * Xóa file Storage và image record PostgreSQL theo imageId.
     *
     * @param imageId UUID của image record cần xóa
     * @return thông tin ảnh đã xóa
     */
    ImageDeleteResponse delete(UUID imageId);
}
