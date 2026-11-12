package org.example.authentication_cookiesession.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Cổng lưu file ảnh, giúp service nghiệp vụ không phụ thuộc trực tiếp vào HTTP
 * API của Supabase.
 */
public interface ImageStorage {

    /**
     * Upload file vào object path đã được backend tạo.
     *
     * @param imageId ID dùng để tạo object path cố định
     * @param file file ảnh đã được validator kiểm tra
     * @return thông tin object đã lưu và public URL
     */
    StoredImage upload(UUID imageId, MultipartFile file);

    /**
     * Xóa object để bù trừ khi bước ghi PostgreSQL thất bại.
     *
     * @param storagePath path tương đối trong bucket, ví dụ {@code images/{imageId}}
     */
    void delete(String storagePath);

    /**
     * Kết quả tối thiểu của một lần upload Storage.
     *
     * @param storagePath path object trong bucket
     * @param publicUrl URL public để lưu vào PostgreSQL
     */
    record StoredImage(String storagePath, String publicUrl) {
    }
}
