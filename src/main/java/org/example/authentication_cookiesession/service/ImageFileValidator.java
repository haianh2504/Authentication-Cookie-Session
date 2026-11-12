package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.exception.ImageUploadException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Kiểm tra file ảnh ở backend trước khi file đi tới Supabase Storage.
 *
 * <p>Không được tin riêng filename, extension hoặc Content-Type do browser gửi.
 * Validator này kiểm tra cả ba lớp đó và đọc magic bytes ở đầu file để phát hiện
 * nội dung thật. Việc đọc signature chỉ dùng một lượng byte nhỏ, không cần nạp
 * toàn bộ file ảnh vào bộ nhớ.</p>
 */
@Component
public class ImageFileValidator {

    public static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024;

    private static final Map<String, Set<String>> MIME_TYPES_BY_EXTENSION = Map.of(
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "heic", Set.of("image/heic", "image/heif"),
            "heif", Set.of("image/heic", "image/heif"),
            "webp", Set.of("image/webp"),
            "avif", Set.of("image/avif")
    );

    private static final Set<String> ALLOWED_EXTENSIONS = MIME_TYPES_BY_EXTENSION.keySet();

    /**
     * Kiểm tra file có đủ điều kiện upload theo UC04 hay không.
     *
     * @param file file multipart từ request
     * @throws ImageUploadException nếu file thiếu, quá lớn hoặc không phải ảnh được cho phép
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException(
                    HttpStatus.BAD_REQUEST,
                    "IMAGE_FILE_REQUIRED",
                    "Image file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImageUploadException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "IMAGE_FILE_TOO_LARGE",
                    "Image file must not exceed 15 MB");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw unsupportedImage();
        }

        if (contentType == null || !MIME_TYPES_BY_EXTENSION.get(extension).contains(contentType)) {
            throw unsupportedImage();
        }

        DetectedImageType detectedType = detectImageType(file);
        if (!detectedType.isCompatibleWith(extension)) {
            throw unsupportedImage();
        }
    }

    /**
     * Lấy extension an toàn từ tên file client gửi lên.
     *
     * <p>Giá trị này chỉ dùng để kiểm tra, tuyệt đối không dùng làm tên object
     * trong Storage.</p>
     */
    private String extractExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    /**
     * Chuẩn hóa Content-Type để so sánh ổn định với allowlist.
     */
    private String normalizeContentType(String contentType) {
        return contentType == null ? null : contentType.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Đọc magic bytes thay vì chỉ tin MIME type từ browser.
     */
    private DetectedImageType detectImageType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(64);
            return detectFromHeader(header);
        } catch (IOException exception) {
            throw new ImageUploadException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "UNSUPPORTED_IMAGE_TYPE",
                    "The uploaded file is not a supported image",
                    exception);
        }
    }

    /**
     * Nhận diện các định dạng được contract cho phép từ phần đầu binary.
     */
    private DetectedImageType detectFromHeader(byte[] header) {
        if (startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return DetectedImageType.JPEG;
        }
        if (startsWith(header, new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return DetectedImageType.PNG;
        }
        if (startsWithAscii(header, 0, "RIFF") && startsWithAscii(header, 8, "WEBP")) {
            return DetectedImageType.WEBP;
        }
        if (startsWithAscii(header, 4, "ftyp")) {
            String brand = ascii(header, 8, 4);
            if (Set.of("avif", "avis").contains(brand)) {
                return DetectedImageType.AVIF;
            }
            if (Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1").contains(brand)) {
                return DetectedImageType.HEIF;
            }
        }
        return DetectedImageType.UNKNOWN;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] value, int offset, String expected) {
        return offset >= 0
                && value.length >= offset + expected.length()
                && expected.equals(ascii(value, offset, expected.length()));
    }

    private String ascii(byte[] value, int offset, int length) {
        if (offset < 0 || value.length < offset + length) {
            return "";
        }
        return new String(value, offset, length, StandardCharsets.US_ASCII);
    }

    private ImageUploadException unsupportedImage() {
        return new ImageUploadException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_IMAGE_TYPE",
                "The uploaded file is not a supported image");
    }

    private enum DetectedImageType {
        JPEG,
        PNG,
        HEIF,
        WEBP,
        AVIF,
        UNKNOWN;

        private boolean isCompatibleWith(String extension) {
            return switch (this) {
                case JPEG -> extension.equals("jpg") || extension.equals("jpeg");
                case PNG -> extension.equals("png");
                case HEIF -> extension.equals("heic") || extension.equals("heif");
                case WEBP -> extension.equals("webp");
                case AVIF -> extension.equals("avif");
                case UNKNOWN -> false;
            };
        }
    }
}
