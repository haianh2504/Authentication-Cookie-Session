package org.example.authentication_cookiesession.controller;

import org.example.authentication_cookiesession.dto.response.ImageUploadResponse;
import org.example.authentication_cookiesession.dto.response.ImageDeleteResponse;
import org.example.authentication_cookiesession.dto.response.ImageItemResponse;
import org.example.authentication_cookiesession.dto.response.ImageListResponse;
import org.example.authentication_cookiesession.dto.response.PaginationResponse;
import org.example.authentication_cookiesession.exception.ApiExceptionHandler;
import org.example.authentication_cookiesession.service.ImageDeletionService;
import org.example.authentication_cookiesession.service.ImageListingService;
import org.example.authentication_cookiesession.service.ImageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    @Mock
    private ImageUploadService imageUploadService;

    @Mock
    private ImageDeletionService imageDeletionService;

    @Mock
    private ImageListingService imageListingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ImageController(
                        imageUploadService,
                        imageDeletionService,
                        imageListingService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    /**
     * Verifies that the first public gallery request omits the cursor and returns
     * the contract response envelope with pagination metadata.
     */
    @Test
    void listsFirstGalleryPageUsingContractEnvelope() throws Exception {
        UUID imageId = UUID.randomUUID();
        ImageListResponse response = new ImageListResponse(
                List.of(new ImageItemResponse(
                        imageId,
                        "https://example.supabase.co/storage/v1/object/public/gallery/images/" + imageId)),
                new PaginationResponse(10, "next-cursor", true));
        when(imageListingService.listImages(null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/images")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("IMAGES_RETRIEVING_SUCCESSFULLY"))
                .andExpect(jsonPath("$.message").value("Retrieving image gallery successfully"))
                .andExpect(jsonPath("$.data.items[0].imageId").value(imageId.toString()))
                .andExpect(jsonPath("$.data.pagination.limit").value(10))
                .andExpect(jsonPath("$.data.pagination.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.data.pagination.hasNextPage").value(true));

        verify(imageListingService).listImages(null);
    }

    /**
     * Verifies that a subsequent gallery request forwards its opaque cursor unchanged.
     */
    @Test
    void listsSubsequentGalleryPageWithOpaqueCursor() throws Exception {
        String cursor = "opaque-cursor";
        ImageListResponse response = new ImageListResponse(
                List.of(),
                new PaginationResponse(10, null, false));
        when(imageListingService.listImages(cursor)).thenReturn(response);

        mockMvc.perform(get("/api/v1/images")
                        .param("nextCursor", cursor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.pagination.nextCursor").value(nullValue()))
                .andExpect(jsonPath("$.data.pagination.hasNextPage").value(false));

        verify(imageListingService).listImages(cursor);
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
    void deletesImageAndReturnsContractEnvelopeForAuthenticatedSession() throws Exception {
        UUID imageId = UUID.randomUUID();
        when(imageDeletionService.delete(imageId))
                .thenReturn(new ImageDeleteResponse(true, imageId));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .principal(authentication)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("IMAGE_DELETING_SUCCESSFULLY"))
                .andExpect(jsonPath("$.data.deleted").value(true))
                .andExpect(jsonPath("$.data.imageId").value(imageId.toString()));

        verify(imageDeletionService).delete(imageId);
    }

    @Test
    void rejectsDeleteWhenSessionAuthenticationIsMissing() throws Exception {
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ADMIN_SESSION_UNAUTHORIZED"));

        verifyNoInteractions(imageDeletionService);
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
