package org.example.authentication_cookiesession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authentication_cookiesession.dto.request.UploadImageRequest;
import org.example.authentication_cookiesession.dto.response.ImageUploadResponse;
import org.example.authentication_cookiesession.exception.ImageStorageException;
import org.example.authentication_cookiesession.exception.ImageUploadException;
import org.example.authentication_cookiesession.repository.ImageRepository;
import org.example.authentication_cookiesession.storage.ImageStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Điều phối luồng ghi ảnh hai nơi: Supabase Storage và Supabase PostgreSQL.
 *
 * <p>Storage được ghi trước, database ghi sau. Vì hai hệ thống không dùng chung
 * transaction, nếu database thất bại sau khi Storage thành công thì service gọi
 * Storage API để cleanup object vừa tạo.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {

    private final ImageFileValidator imageFileValidator;
    private final ImageStorage imageStorage;
    private final ImageRepository imageRepository;

    /**
     * Thực hiện toàn bộ UC04 sau khi Spring Security đã kiểm tra session, role và CSRF.
     */
    @Override
    public ImageUploadResponse upload(UploadImageRequest request) {
        MultipartFile file = request == null ? null : request.file();
        imageFileValidator.validate(file);

        UUID imageId = UUID.randomUUID();
        ImageStorage.StoredImage storedImage = imageStorage.upload(imageId, file);

        try {
            // created_at không truyền vào để PostgreSQL dùng DEFAULT now() trong V1.
            imageRepository.insertUploadedImage(imageId, storedImage.publicUrl());
            return new ImageUploadResponse(imageId, storedImage.publicUrl());
        } catch (RuntimeException persistenceException) {
            cleanupAfterDatabaseFailure(imageId, storedImage.storagePath(), persistenceException);
            throw new ImageUploadException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "IMAGE_UPLOAD_FAILED",
                    "The image could not be saved",
                    persistenceException);
        }
    }

    /**
     * Xóa object Storage vừa tạo khi insert PostgreSQL thất bại.
     *
     * <p>Cleanup là best effort vì Supabase Storage và PostgreSQL không có
     * distributed transaction. Nếu cleanup thất bại, log chỉ ghi ID/path để có
     * thể reconciliation; không ghi service-role key hay nội dung file.</p>
     */
    private void cleanupAfterDatabaseFailure(
            UUID imageId,
            String storagePath,
            RuntimeException persistenceException) {
        try {
            imageStorage.delete(storagePath);
        } catch (ImageStorageException cleanupException) {
            log.error(
                    "Image persistence failed and Storage cleanup also failed; reconciliation required for imageId={} storagePath={}",
                    imageId,
                    storagePath,
                    cleanupException);
        }

        log.error(
                "Image database insert failed after Storage upload for imageId={} storagePath={}",
                imageId,
                storagePath,
                persistenceException);
    }
}
