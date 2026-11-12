package org.example.authentication_cookiesession.controller;

import org.example.authentication_cookiesession.dto.response.ImageUploadResponse;
import org.example.authentication_cookiesession.exception.ApiExceptionHandler;
import org.example.authentication_cookiesession.service.ImageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    @Mock
    private ImageUploadService imageUploadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ImageController(imageUploadService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void acceptsMultipartFieldFileAndReturnsContractEnvelope() throws Exception {
        UUID imageId = UUID.randomUUID();
        when(imageUploadService.upload(any()))
                .thenReturn(new ImageUploadResponse(
                        imageId,
                        "https://example.supabase.co/storage/v1/object/public/gallery/images/" + imageId));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ronaldo.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/images").file(file)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("IMAGE_UPLOAD_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.imageId").value(imageId.toString()))
                .andExpect(jsonPath("$.data.imageURL").isNotEmpty());

        verify(imageUploadService).upload(any());
    }

    @Test
    void returnsBadRequestWhenMultipartFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/images")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
