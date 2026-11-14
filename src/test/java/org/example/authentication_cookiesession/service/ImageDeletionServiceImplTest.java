package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.dto.response.ImageDeleteResponse;
import org.example.authentication_cookiesession.entity.Image;
import org.example.authentication_cookiesession.exception.ImageDeletionException;
import org.example.authentication_cookiesession.exception.ImageStorageException;
import org.example.authentication_cookiesession.repository.ImageRepository;
import org.example.authentication_cookiesession.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageDeletionServiceImplTest {

    @Mock
    private ImageStorage imageStorage;

    @Mock
    private ImageRepository imageRepository;

    private ImageDeletionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImageDeletionServiceImpl(imageStorage, imageRepository);
    }

    @Test
    void deletesStorageBeforeDatabaseRecord() {
        UUID imageId = UUID.randomUUID();
        Image image = image(imageId);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(imageRepository.deleteImageById(imageId)).thenReturn(1);

        ImageDeleteResponse response = service.delete(imageId);

        assertTrue(response.deleted());
        assertEquals(imageId, response.imageId());
        InOrder inOrder = inOrder(imageStorage, imageRepository);
        inOrder.verify(imageStorage).delete("images/" + imageId);
        inOrder.verify(imageRepository).deleteImageById(imageId);
    }

    @Test
    void doesNotDeleteDatabaseRecordWhenStorageDeletionFails() {
        UUID imageId = UUID.randomUUID();
        Image image = image(imageId);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        doThrow(new ImageStorageException("storage unavailable", new IllegalStateException()))
                .when(imageStorage)
                .delete("images/" + imageId);

        ImageDeletionException exception = assertThrows(
                ImageDeletionException.class,
                () -> service.delete(imageId));

        assertEquals(500, exception.getStatus().value());
        assertEquals("IMAGE_DELETION_FAILED", exception.getCode());
        verify(imageRepository, never()).deleteImageById(imageId);
    }

    @Test
    void returnsNotFoundWithoutCallingStorageWhenDatabaseRecordDoesNotExist() {
        UUID imageId = UUID.randomUUID();
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        ImageDeletionException exception = assertThrows(
                ImageDeletionException.class,
                () -> service.delete(imageId));

        assertEquals(404, exception.getStatus().value());
        verifyNoInteractions(imageStorage);
    }

    @Test
    void wrapsDatabaseFailureAfterStorageDeletionSoTransactionCanRollback() {
        UUID imageId = UUID.randomUUID();
        Image image = image(imageId);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        doThrow(new IllegalStateException("database unavailable"))
                .when(imageRepository)
                .deleteImageById(imageId);

        ImageDeletionException exception = assertThrows(
                ImageDeletionException.class,
                () -> service.delete(imageId));

        assertEquals("IMAGE_DELETION_FAILED", exception.getCode());
        verify(imageStorage).delete("images/" + imageId);
    }

    /**
     * Delete service phải coi kết quả xóa database khác một row là lỗi bất thường.
     */
    // Kiểm tra race condition hoặc repository anomaly không được trả thành công giả.
    @Test
    void rejectsUnexpectedDatabaseDeleteCountAfterStorageDeletion() {
        UUID imageId = UUID.randomUUID();
        Image image = image(imageId);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(imageRepository.deleteImageById(imageId)).thenReturn(0);

        ImageDeletionException exception = assertThrows(
                ImageDeletionException.class,
                () -> service.delete(imageId));

        assertEquals(500, exception.getStatus().value());
        assertEquals("IMAGE_DELETION_FAILED", exception.getCode());
        verify(imageStorage).delete("images/" + imageId);
        verify(imageRepository).deleteImageById(imageId);
    }

    @Test
    void deleteMethodIsTransactional() throws NoSuchMethodException {
        assertTrue(ImageDeletionServiceImpl.class
                .getMethod("delete", UUID.class)
                .isAnnotationPresent(Transactional.class));
    }

    private Image image(UUID imageId) {
        Image image = mock(Image.class);
        when(image.getId()).thenReturn(imageId);
        return image;
    }
}
