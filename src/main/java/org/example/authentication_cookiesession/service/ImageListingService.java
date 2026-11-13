package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.dto.response.ImageListResponse;

/**
 * Nghiệp vụ đọc danh sách ảnh công khai của use case UC01.
 */
public interface ImageListingService {

    /**
     * Lấy một trang ảnh theo cursor opaque.
     *
     * @param nextCursor cursor của trang trước; truyền {@code null} cho trang đầu tiên
     * @return danh sách ảnh và metadata phân trang theo API contract
     */
    ImageListResponse listImages(String nextCursor);
}
