package org.example.authentication_cookiesession.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authentication_cookiesession.dto.response.ImageDeleteResponse;
import org.example.authentication_cookiesession.entity.Image;
import org.example.authentication_cookiesession.exception.ImageDeletionException;
import org.example.authentication_cookiesession.exception.ImageStorageException;
import org.example.authentication_cookiesession.repository.ImageRepository;
import org.example.authentication_cookiesession.storage.ImageStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Điều phối việc xóa cứng image record và Storage object.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDeletionServiceImpl implements ImageDeletionService {

    private final ImageStorage imageStorage;
    private final ImageRepository imageRepository;

    /**
     * Storage được xóa trước theo contract. PostgreSQL nằm trong một transaction
     * và được flush ngay trước khi service trả kết quả để lỗi database làm rollback.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImageDeleteResponse delete(UUID imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> ImageDeletionException.notFound());

        String storagePath = "images/" + image.getId();
        try {
            imageStorage.delete(storagePath);
        } catch (ImageStorageException storageException) {
            throw new ImageDeletionException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "IMAGE_DELETION_FAILED",
                    "The image could not be deleted",
                    storageException);
        }

        try {
            int deletedRows = imageRepository.deleteImageById(image.getId());
            if (deletedRows != 1) {
                throw new ImageDeletionException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "IMAGE_DELETION_FAILED",
                        "The image could not be deleted");
            }
        } catch (ImageDeletionException deletionException) {
            throw deletionException;
        } catch (RuntimeException databaseException) {
            log.error(
                    "Image database deletion failed after Storage deletion for imageId={} storagePath={}",
                    image.getId(),
                    storagePath,
                    databaseException);
            throw new ImageDeletionException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "IMAGE_DELETION_FAILED",
                    "The image could not be deleted",
                    databaseException);
        }

        return new ImageDeleteResponse(true, image.getId());
    }
}
