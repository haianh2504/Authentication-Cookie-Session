package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.dto.request.UploadImageRequest;
import org.example.authentication_cookiesession.dto.response.ImageUploadResponse;

/**
 * Nghiệp vụ chính của use case Upload an image.
 */
public interface ImageUploadService {

    /**
     * Validate và lưu một ảnh vào Supabase Storage cùng PostgreSQL.
     *
     * @param request DTO multipart từ controller
     * @return imageId và public imageURL theo API contract
     */
    ImageUploadResponse upload(UploadImageRequest request);
}
