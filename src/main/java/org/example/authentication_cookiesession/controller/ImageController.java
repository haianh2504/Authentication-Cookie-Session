package org.example.authentication_cookiesession.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.dto.request.UploadImageRequest;
import org.example.authentication_cookiesession.dto.response.ApiResponse;
import org.example.authentication_cookiesession.dto.response.ImageUploadResponse;
import org.example.authentication_cookiesession.service.ImageUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller cho các thao tác ảnh của gallery.
 *
 * <p>Quyền ADMIN và CSRF được kiểm tra ở Spring Security trước khi method upload
 * được gọi; controller chỉ nhận DTO, gọi service và tạo response envelope.</p>
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageUploadService imageUploadService;

    /**
     * Upload file ảnh bằng multipart field {@code file}.
     * @param request DTO request được Spring bind từ multipart form
     * @return response 201 chứa imageId và imageURL
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @Valid @ModelAttribute UploadImageRequest request) {
        ImageUploadResponse data = imageUploadService.upload(request);
        ApiResponse<ImageUploadResponse> body = ApiResponse.of(
                HttpStatus.CREATED.value(),
                "IMAGE_UPLOAD_SUCCESSFULLY",
                "Image uploaded successfully",
                data);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
