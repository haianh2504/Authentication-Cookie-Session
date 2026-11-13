package org.example.authentication_cookiesession.dto.response;

import java.util.List;

/**
 * Dữ liệu use case UC01 gồm các ảnh của trang hiện tại và metadata phân trang.
 *
 * @param items danh sách ảnh được sắp xếp mới nhất trước
 * @param pagination thông tin cursor và trạng thái trang kế tiếp
 */
public record ImageListResponse(
        List<ImageItemResponse> items,
        PaginationResponse pagination
) {
}
