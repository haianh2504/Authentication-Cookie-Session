package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.dto.response.ImageListResponse;
import org.example.authentication_cookiesession.entity.Image;
import org.example.authentication_cookiesession.repository.ImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho nghiệp vụ cursor pagination, không khởi động Spring context
 * và không kết nối database thật.
 */
@ExtendWith(MockitoExtension.class)
class ImageListingServiceImplTest {

    @Mock
    private ImageRepository imageRepository;

    /**
     * Kiểm tra page đầu lấy dư một record, chỉ trả 10 item và tạo nextCursor.
     */
    @Test
    void returnsTenItemsAndNextCursorWhenMoreImagesExist() {
        ImageListingServiceImpl service = new ImageListingServiceImpl(imageRepository);
        List<Image> fetchedImages = images(11);
        when(imageRepository.findFirstPage(any(Pageable.class))).thenReturn(fetchedImages);

        ImageListResponse response = service.listImages(null);

        assertEquals(10, response.items().size());
        assertEquals(fetchedImages.get(0).getId(), response.items().get(0).imageId());
        assertEquals(fetchedImages.get(9).getImageUrl(), response.items().get(9).imageURL());
        assertEquals(10, response.pagination().limit());
        assertTrue(response.pagination().hasNextPage());
        assertNotNull(response.pagination().nextCursor());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(imageRepository).findFirstPage(pageableCaptor.capture());
        assertEquals(11, pageableCaptor.getValue().getPageSize());
    }

    /**
     * Kiểm tra service giải mã cursor và chuyển đúng vị trí sang repository.
     */
    @Test
    void returnsRemainingItemsAfterOpaqueCursor() {
        ImageListingServiceImpl service = new ImageListingServiceImpl(imageRepository);
        OffsetDateTime cursorCreatedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        UUID cursorImageId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        String cursor = encodeCursor(cursorCreatedAt, cursorImageId);
        List<Image> remainingImages = images(2);
        when(imageRepository.findPageAfterCursor(
                eq(cursorCreatedAt),
                eq(cursorImageId),
                any(Pageable.class)))
                .thenReturn(remainingImages);

        ImageListResponse response = service.listImages(cursor);

        assertEquals(2, response.items().size());
        assertFalse(response.pagination().hasNextPage());
        assertEquals(10, response.pagination().limit());
        assertNull(response.pagination().nextCursor());
        verify(imageRepository).findPageAfterCursor(
                eq(cursorCreatedAt),
                eq(cursorImageId),
                any(Pageable.class));
    }

    /**
     * Kiểm tra cursor sai bị từ chối trước khi service gọi database.
     */
    @Test
    void rejectsMalformedCursorBeforeQueryingRepository() {
        ImageListingServiceImpl service = new ImageListingServiceImpl(imageRepository);

        assertThrows(IllegalArgumentException.class, () -> service.listImages("invalid-cursor"));

        verifyNoInteractions(imageRepository);
    }

    /**
     * Tạo entity mock có đủ field mà service dùng để map response và tạo cursor.
     */
    private List<Image> images(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    Image image = org.mockito.Mockito.mock(Image.class);
                    lenient().when(image.getId()).thenReturn(UUID.fromString(String.format(
                            "00000000-0000-0000-0000-%012d", index + 1)));
                    lenient().when(image.getImageUrl()).thenReturn("https://example.com/images/" + (index + 1));
                    lenient().when(image.getCreatedAt()).thenReturn(
                            OffsetDateTime.parse("2026-01-01T10:00:00Z").minusMinutes(index));
                    return image;
                })
                .toList();
    }

    /**
     * Tạo cursor deterministic đúng encoding mà implementation sử dụng.
     */
    private String encodeCursor(OffsetDateTime createdAt, UUID imageId) {
        String value = createdAt + "|" + imageId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
