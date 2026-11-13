package org.example.authentication_cookiesession.service;

import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.dto.response.ImageItemResponse;
import org.example.authentication_cookiesession.dto.response.ImageListResponse;
import org.example.authentication_cookiesession.dto.response.PaginationResponse;
import org.example.authentication_cookiesession.entity.Image;
import org.example.authentication_cookiesession.repository.ImageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Điều phối việc đọc gallery bằng keyset pagination.
 *
 * <p>Cursor được tạo từ cặp {@code created_at} và {@code id}. Cặp này khớp với
 * thứ tự ổn định đã quy định trong tài liệu: ảnh mới hơn đứng trước, còn
 * {@code id} là tie-breaker khi nhiều ảnh có cùng thời điểm tạo.</p>
 */
// Đăng ký implementation này thành Spring bean để controller có thể inject qua interface.
@Service
// Tự sinh constructor chứa các dependency bắt buộc và tránh viết boilerplate constructor.
@RequiredArgsConstructor
public class ImageListingServiceImpl implements ImageListingService {

    private static final int PAGE_SIZE = 10;
    private static final int FETCH_SIZE = PAGE_SIZE + 1;
    private static final String CURSOR_SEPARATOR = "|";

    private final ImageRepository imageRepository;

    /**
     * Đọc trang hiện tại, lấy dư một record để xác định còn trang kế tiếp hay không.
     *
     * @param nextCursor cursor opaque của trang trước hoặc {@code null} cho request đầu tiên
     * @return response data gồm items và pagination metadata
     */
    @Override
    // Chỉ đọc dữ liệu trong một transaction, không thay đổi record gallery.
    @Transactional(readOnly = true)
    public ImageListResponse listImages(String nextCursor) {
        List<Image> fetchedImages = fetchImages(nextCursor);
        boolean hasNextPage = fetchedImages.size() > PAGE_SIZE;
        List<Image> currentPage = hasNextPage
                ? fetchedImages.subList(0, PAGE_SIZE)
                : fetchedImages;

        String nextPageCursor = hasNextPage
                ? encodeCursor(currentPage.get(currentPage.size() - 1))
                : null;

        List<ImageItemResponse> items = currentPage.stream()
                .map(this::toImageItemResponse)
                .toList();

        return new ImageListResponse(
                items,
                new PaginationResponse(PAGE_SIZE, nextPageCursor, hasNextPage));
    }

    /**
     * Chọn query cho trang đầu hoặc query tiếp theo dựa trên cursor đã giải mã.
     */
    private List<Image> fetchImages(String nextCursor) {
        Pageable fetchLimit = PageRequest.of(0, FETCH_SIZE);

        if (nextCursor == null || nextCursor.isBlank()) {
            return imageRepository.findFirstPage(fetchLimit);
        }

        CursorPosition cursor = decodeCursor(nextCursor);
        return imageRepository.findPageAfterCursor(
                cursor.createdAt(),
                cursor.imageId(),
                fetchLimit);
    }

    /**
     * Chuyển entity database thành DTO public, không để lộ entity JPA ra ngoài service.
     */
    private ImageItemResponse toImageItemResponse(Image image) {
        return new ImageItemResponse(image.getId(), image.getImageUrl());
    }

    /**
     * Tạo cursor opaque từ vị trí cuối cùng của trang hiện tại.
     *
     * <p>Client chỉ lưu và gửi lại chuỗi này; client không cần biết cấu trúc bên trong.</p>
     */
    private String encodeCursor(Image image) {
        String cursorValue = image.getCreatedAt()
                + CURSOR_SEPARATOR
                + image.getId();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(cursorValue.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Giải mã cursor thành vị trí database dùng cho điều kiện keyset pagination.
     *
     * @throws IllegalArgumentException nếu cursor không đúng định dạng opaque của server
     */
    private CursorPosition decodeCursor(String nextCursor) {
        try {
            String decodedCursor = new String(
                    Base64.getUrlDecoder().decode(nextCursor),
                    StandardCharsets.UTF_8);
            String[] parts = decodedCursor.split(Pattern.quote(CURSOR_SEPARATOR), -1);

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid nextCursor");
            }

            return new CursorPosition(
                    OffsetDateTime.parse(parts[0]),
                    UUID.fromString(parts[1]));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid nextCursor", exception);
        }
    }

    /**
     * Vị trí cuối cùng của một trang theo đúng hai cột sort của database.
     */
    private record CursorPosition(OffsetDateTime createdAt, UUID imageId) {
    }
}
