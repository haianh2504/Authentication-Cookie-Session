package org.example.authentication_cookiesession.repository;

import org.example.authentication_cookiesession.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    /**
     * Ghi image record sau khi Storage upload thành công.
     *
     * <p>Không truyền created_at để PostgreSQL dùng đúng DEFAULT now() trong
     * migration V1. ID được truyền từ service để trùng với object path Storage.</p>
     *
     * @param imageId UUID đã được tạo trước khi upload
     * @param imageUrl public URL của object Storage
     */
    @Modifying(flushAutomatically = true)
    @Transactional
    @Query(value = "INSERT INTO images (id, image_url) VALUES (:imageId, :imageUrl)", nativeQuery = true)
    void insertUploadedImage(
            @Param("imageId") UUID imageId,
            @Param("imageUrl") String imageUrl);

    @Query("select image.imageUrl from Image image order by image.createdAt desc, image.id desc")
    List<String> findImageUrlsOrderByCreatedAtDesc();
}
