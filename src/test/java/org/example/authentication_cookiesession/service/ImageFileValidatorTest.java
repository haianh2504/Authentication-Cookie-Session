package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.exception.ImageUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageFileValidatorTest {

    private final ImageFileValidator validator = new ImageFileValidator();

    @Test
    void acceptsPngWhenExtensionMimeTypeAndMagicBytesMatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ronaldo.png",
                "image/png",
                new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00
                });

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void rejectsFileWhenFilenameAndMimeTypeLookLikeImageButBytesDoNot() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                "not an image".getBytes(StandardCharsets.UTF_8));

        ImageUploadException exception = assertThrows(
                ImageUploadException.class,
                () -> validator.validate(file));

        assertEquals(415, exception.getStatus().value());
        assertEquals("UNSUPPORTED_IMAGE_TYPE", exception.getCode());
    }

    @Test
    void rejectsFileLargerThanFifteenMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                new byte[(int) ImageFileValidator.MAX_FILE_SIZE_BYTES + 1]);

        ImageUploadException exception = assertThrows(
                ImageUploadException.class,
                () -> validator.validate(file));

        assertEquals(413, exception.getStatus().value());
        assertEquals("IMAGE_FILE_TOO_LARGE", exception.getCode());
    }

    /**
     * Validator phải coi null và multipart rỗng là lỗi thiếu file 400.
     */
    // Kiểm tra hai dạng request không có dữ liệu ảnh thực tế.
    @Test
    void rejectsNullAndEmptyFilesAsRequiredInput() {
        ImageUploadException nullException = assertThrows(
                ImageUploadException.class,
                () -> validator.validate(null));
        ImageUploadException emptyException = assertThrows(
                ImageUploadException.class,
                () -> validator.validate(new MockMultipartFile(
                        "file", "empty.png", "image/png", new byte[0])));

        assertEquals(400, nullException.getStatus().value());
        assertEquals("IMAGE_FILE_REQUIRED", nullException.getCode());
        assertEquals(400, emptyException.getStatus().value());
        assertEquals("IMAGE_FILE_REQUIRED", emptyException.getCode());
    }

    /**
     * Validator phải chấp nhận toàn bộ allowlist khi metadata và magic bytes khớp.
     */
    // Kiểm tra đủ sáu nhóm định dạng được contract cho phép, gồm cả extension viết hoa.
    @ParameterizedTest
    @MethodSource("supportedImageFiles")
    void acceptsEveryContractSupportedImageFormat(
            String extension,
            String contentType,
            byte[] bytes) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image." + extension.toUpperCase(Locale.ROOT),
                " " + contentType.toUpperCase(Locale.ROOT) + " ",
                bytes);

        assertDoesNotThrow(() -> validator.validate(file));
    }

    /**
     * Validator phải từ chối extension hoặc Content-Type nằm ngoài allowlist.
     */
    // Kiểm tra client không thể khai báo một định dạng server không hỗ trợ.
    @ParameterizedTest
    @MethodSource("unsupportedImageMetadata")
    void rejectsUnsupportedExtensionOrMimeType(
            String filename,
            String contentType) {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, contentType, pngBytes());

        ImageUploadException exception = assertThrows(
                ImageUploadException.class,
                () -> validator.validate(file));

        assertEquals(415, exception.getStatus().value());
        assertEquals("UNSUPPORTED_IMAGE_TYPE", exception.getCode());
    }

    /**
     * File đúng kích thước tối đa phải được chấp nhận nếu các lớp kiểm tra khác hợp lệ.
     */
    // Kiểm tra boundary 15 MB là inclusive theo contract.
    @Test
    void acceptsFileExactlyAtFifteenMegabytes() {
        byte[] bytes = new byte[(int) ImageFileValidator.MAX_FILE_SIZE_BYTES];
        System.arraycopy(pngBytes(), 0, bytes, 0, pngBytes().length);
        MockMultipartFile file = new MockMultipartFile(
                "file", "maximum.png", "image/png", bytes);

        assertDoesNotThrow(() -> validator.validate(file));
    }

    /**
     * Lỗi đọc stream phải trở thành lỗi media an toàn thay vì lộ IOException.
     */
    // Kiểm tra validator xử lý MultipartFile hỏng hoặc stream không đọc được.
    @Test
    void rejectsFileWhenContentStreamCannotBeRead() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenThrow(new IOException("read failed"));

        ImageUploadException exception = assertThrows(
                ImageUploadException.class,
                () -> validator.validate(file));

        assertEquals(415, exception.getStatus().value());
        assertEquals("UNSUPPORTED_IMAGE_TYPE", exception.getCode());
    }

    /**
     * Cung cấp fixture magic bytes cho mọi định dạng mà contract cho phép.
     */
    // Tạo các header tối thiểu để test tập trung vào nhận diện định dạng.
    private static Stream<Arguments> supportedImageFiles() {
        return Stream.of(
                Arguments.of("jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
                Arguments.of("jpeg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
                Arguments.of("png", "image/png", pngBytes()),
                Arguments.of("heic", "image/heic", isoBrandBytes("heic")),
                Arguments.of("heif", "image/heif", isoBrandBytes("mif1")),
                Arguments.of("webp", "image/webp", webpBytes()),
                Arguments.of("avif", "image/avif", isoBrandBytes("avif")));
    }

    /**
     * Cung cấp metadata không được phép để kiểm tra allowlist bị khóa chặt.
     */
    // Tách fixture metadata khỏi test để nhìn rõ các biến thể bị từ chối.
    private static Stream<Arguments> unsupportedImageMetadata() {
        return Stream.of(
                Arguments.of("image.gif", "image/gif"),
                Arguments.of("image.exe", "image/png"),
                Arguments.of("image.png", "image/jpeg"),
                Arguments.of("image.png", null),
                Arguments.of("image", "image/png"));
    }

    /**
     * Tạo PNG header tối thiểu dùng chung cho fixture validator.
     */
    // Giữ fixture nhỏ để test không phụ thuộc thư viện xử lý ảnh.
    private static byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00};
    }

    /**
     * Tạo container header tối thiểu có brand ISO BMFF tại offset chuẩn.
     */
    // Dùng cho HEIC/HEIF/AVIF mà không cần file binary lớn.
    private static byte[] isoBrandBytes(String brand) {
        byte[] bytes = new byte[12];
        bytes[4] = 'f';
        bytes[5] = 't';
        bytes[6] = 'y';
        bytes[7] = 'p';
        byte[] brandBytes = brand.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(brandBytes, 0, bytes, 8, 4);
        return bytes;
    }

    /**
     * Tạo RIFF/WEBP header tối thiểu.
     */
    // Dùng đúng hai marker mà validator cần đọc ở offset 0 và 8.
    private static byte[] webpBytes() {
        byte[] bytes = new byte[12];
        bytes[0] = 'R';
        bytes[1] = 'I';
        bytes[2] = 'F';
        bytes[3] = 'F';
        bytes[8] = 'W';
        bytes[9] = 'E';
        bytes[10] = 'B';
        bytes[11] = 'P';
        return bytes;
    }
}
