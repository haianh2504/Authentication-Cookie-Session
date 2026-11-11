package org.example.authentication_cookiesession.exception;

import org.example.authentication_cookiesession.dto.response.ApiResponse;
import org.example.authentication_cookiesession.dto.response.ValidationError;
import org.example.authentication_cookiesession.dto.response.ValidationErrorsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Chuẩn hóa lỗi phát sinh trong controller thành API response envelope.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Chuyển lỗi Bean Validation thành danh sách lỗi theo từng field.
     *
     * @param exception exception do @Valid tạo ra
     * @return response 400 theo format validation error trong contract
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorsResponse>> handleValidationException(
            MethodArgumentNotValidException exception) {
        List<ValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        ApiResponse<ValidationErrorsResponse> body = ApiResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "The request contains invalid values",
                new ValidationErrorsResponse(errors));

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Che giấu chi tiết authentication failure và dùng một thông báo chung.
     *
     * @param exception authentication exception từ AuthenticationManager
     * @return response 401 theo contract login
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthenticationException(
            AuthenticationException exception) {
        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Invalid email or password",
                Map.of());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Chuyển lỗi ngoài dự kiến thành response generic, không lộ exception nội bộ.
     *
     * @param exception exception ngoài dự kiến trong quá trình xử lý request
     * @return response 500 an toàn cho client
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleUnexpectedException(Exception exception) {
        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "The server could not process the request",
                Map.of());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Chuyển FieldError của Spring thành DTO validation của API.
     *
     * @param fieldError lỗi validation của một request field
     * @return DTO lỗi gồm field và message
     */
    private ValidationError toValidationError(FieldError fieldError) {
        return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
