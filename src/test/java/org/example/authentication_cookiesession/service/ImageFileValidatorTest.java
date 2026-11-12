package org.example.authentication_cookiesession.service;

import org.example.authentication_cookiesession.exception.ImageUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
