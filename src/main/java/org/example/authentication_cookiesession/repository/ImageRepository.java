package org.example.authentication_cookiesession.repository;

import org.example.authentication_cookiesession.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
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

    /**
     * Xóa cứng image record theo đúng UUID của ảnh.
     * @param imageId UUID của image record
     * @return số record đã bị xóa
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from Image image where image.id = :imageId")
    int deleteImageById(@Param("imageId") UUID imageId);

    @Query("select image.imageUrl from Image image order by image.createdAt desc, image.id desc")
    List<String> findImageUrlsOrderByCreatedAtDesc();

    /**
     * Lấy trang đầu tiên theo thứ tự newest-first mà không cần chạy count query.
     *
     * @param pageable giới hạn số record cần lấy, service truyền 11 record
     * @return tối đa số record được giới hạn bởi pageable
     */
    @Query("select image from Image image order by image.createdAt desc, image.id desc")
    List<Image> findFirstPage(Pageable pageable);

    /**
     * Lấy trang ảnh sau vị trí cursor theo keyset pagination.
     *
     * <p>Điều kiện dùng {@code createdAt} làm khóa chính để sắp xếp và {@code imageId}
     * làm tie-breaker, đúng với thứ tự newest-first của UC01.</p>
     *
     * @param createdAt thời điểm của record cuối trang trước
     * @param imageId ID của record cuối trang trước
     * @param pageable giới hạn số record cần lấy, service truyền 11 record
     * @return tối đa số record được giới hạn bởi pageable
     */
    @Query("""
            select image
            from Image image
            where image.createdAt < :createdAt
               or (image.createdAt = :createdAt and image.id < :imageId)
            order by image.createdAt desc, image.id desc
            """)
    List<Image> findPageAfterCursor(
            @Param("createdAt") OffsetDateTime createdAt,
            @Param("imageId") UUID imageId,
            Pageable pageable);
}
