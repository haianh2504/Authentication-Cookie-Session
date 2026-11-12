package org.example.authentication_cookiesession.exception;

import org.example.authentication_cookiesession.dto.response.ApiResponse;
import org.example.authentication_cookiesession.dto.response.ValidationError;
import org.example.authentication_cookiesession.dto.response.ValidationErrorsResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.example.authentication_cookiesession.exception.ImageUploadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Chuẩn hóa lỗi phát sinh trong controller thành API response envelope.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    /**
     * Trả lỗi validation riêng cho các quy tắc file upload.
     *
     * @param exception lỗi có status/code/message an toàn do validator tạo ra
     * @return response lỗi theo common envelope
     */
    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleImageUploadException(
            ImageUploadException exception) {
        if (exception.getStatus().is5xxServerError()) {
            log.error("Image upload failed with code={}", exception.getCode(), exception);
        }

        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                exception.getStatus().value(),
                exception.getCode(),
                exception.getClientMessage(),
                Map.of());

        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    /**
     * Trả lỗi an toàn cho use case xóa ảnh.
     */
    @ExceptionHandler(ImageDeletionException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleImageDeletionException(
            ImageDeletionException exception) {
        if (exception.getStatus().is5xxServerError()) {
            log.error("Image deletion failed with code={}", exception.getCode(), exception);
        }

        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                exception.getStatus().value(),
                exception.getCode(),
                exception.getClientMessage(),
                Map.of());

        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    /**
     * Bắt lỗi kích thước multipart do container từ chối trước khi vào controller.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception) {
        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "IMAGE_FILE_TOO_LARGE",
                "Image file must not exceed 15 MB",
                Map.of());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    /**
     * Chuẩn hóa lỗi multipart bị thiếu hoặc không đọc được thành 400.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMultipartException(
            MultipartException exception) {
        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "IMAGE_FILE_REQUIRED",
                "Image file is required",
                Map.of());

        return ResponseEntity.badRequest().body(body);
    }

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
        log.error("Unhandled API exception", exception);

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
