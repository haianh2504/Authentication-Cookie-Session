package org.example.authentication_cookiesession.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO nhận request multipart của use case Upload an image.
 *
 * @param file file ảnh nằm trong multipart field có tên chính xác là {@code file}
 */
public record UploadImageRequest(
        @NotNull(message = "File is required") MultipartFile file
) {
}
