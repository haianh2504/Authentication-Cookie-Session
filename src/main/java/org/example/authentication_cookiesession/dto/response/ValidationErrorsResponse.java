package org.example.authentication_cookiesession.dto.response;

import java.util.List;

/**
 * Phần data của response validation error theo API contract.
 *
 * @param errors danh sách lỗi theo từng field
 */
public record ValidationErrorsResponse(List<ValidationError> errors) {
}
