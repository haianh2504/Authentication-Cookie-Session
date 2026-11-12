package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.dto.request.UploadImageRequest;
import org.example.authentication_cookiesession.dto.response.ImageUploadResponse;
import org.example.authentication_cookiesession.exception.ImageStorageException;
import org.example.authentication_cookiesession.exception.ImageUploadException;
import org.example.authentication_cookiesession.repository.ImageRepository;
import org.example.authentication_cookiesession.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceImplTest {

    @Mock
    private ImageFileValidator imageFileValidator;

    @Mock
    private ImageStorage imageStorage;

    @Mock
    private ImageRepository imageRepository;

    @Test
    void uploadsToStorageBeforePersistingDatabaseRecord() {
        ImageUploadServiceImpl service = new ImageUploadServiceImpl(
                imageFileValidator,
                imageStorage,
                imageRepository);
        MockMultipartFile file = validPng();
        UUID imageId = UUID.randomUUID();
        ImageStorage.StoredImage storedImage = new ImageStorage.StoredImage(
                "images/" + imageId,
                "https://example.supabase.co/storage/v1/object/public/gallery/images/" + imageId);

        when(imageStorage.upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(file)))
                .thenReturn(storedImage);

        ImageUploadResponse response = service.upload(new UploadImageRequest(file));

        assertEquals(storedImage.publicUrl(), response.imageURL());
        verify(imageRepository).insertUploadedImage(response.imageId(), storedImage.publicUrl());
        InOrder inOrder = inOrder(imageStorage, imageRepository);
        inOrder.verify(imageStorage).upload(response.imageId(), file);
        inOrder.verify(imageRepository).insertUploadedImage(response.imageId(), storedImage.publicUrl());
    }

    @Test
    void deletesStorageObjectWhenDatabaseInsertFails() {
        ImageUploadServiceImpl service = new ImageUploadServiceImpl(
                imageFileValidator,
                imageStorage,
                imageRepository);
        MockMultipartFile file = validPng();
        ImageStorage.StoredImage storedImage = new ImageStorage.StoredImage(
                "images/temporary-id",
                "https://example.supabase.co/storage/v1/object/public/gallery/images/temporary-id");

        when(imageStorage.upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(file)))
                .thenReturn(storedImage);
        doThrow(new IllegalStateException("database unavailable"))
                .when(imageRepository)
                .insertUploadedImage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());

        ImageUploadException exception = assertThrows(
                ImageUploadException.class,
                () -> service.upload(new UploadImageRequest(file)));

        assertEquals(500, exception.getStatus().value());
        verify(imageStorage).delete(storedImage.storagePath());
    }

    @Test
    void stillReturnsSafeErrorWhenStorageCleanupFails() {
        ImageUploadServiceImpl service = new ImageUploadServiceImpl(
                imageFileValidator,
                imageStorage,
                imageRepository);
        MockMultipartFile file = validPng();
        ImageStorage.StoredImage storedImage = new ImageStorage.StoredImage(
                "images/temporary-id",
                "https://example.supabase.co/storage/v1/object/public/gallery/images/temporary-id");

        when(imageStorage.upload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(file)))
                .thenReturn(storedImage);
        doThrow(new IllegalStateException("database unavailable"))
                .when(imageRepository)
                .insertUploadedImage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        doThrow(new ImageStorageException("storage unavailable", new IllegalStateException()))
                .when(imageStorage)
                .delete(storedImage.storagePath());

        ImageUploadException exception = assertThrows(
                ImageUploadException.class,
                () -> service.upload(new UploadImageRequest(file)));

        assertEquals("IMAGE_UPLOAD_FAILED", exception.getCode());
        verify(imageStorage).delete(storedImage.storagePath());
    }

    private MockMultipartFile validPng() {
        return new MockMultipartFile(
                "file",
                "ronaldo.png",
                "image/png",
                new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00
                });
    }
}
