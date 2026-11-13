package org.example.authentication_cookiesession.dto.response;

/**
 * Metadata phân trang cursor của response gallery.
 *
 * @param limit số lượng item tối đa mà server cho phép trong một trang
 * @param nextCursor cursor opaque dùng để lấy trang kế tiếp; {@code null} khi hết dữ liệu
 * @param hasNextPage cho biết còn trang dữ liệu kế tiếp hay không
 */
public record PaginationResponse(
        int limit,
        String nextCursor,
        boolean hasNextPage
) {
}
