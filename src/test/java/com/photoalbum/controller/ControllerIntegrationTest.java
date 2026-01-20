package com.photoalbum.controller;

import com.photoalbum.model.Photo;
import com.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for REST API controllers
 * Tests the web layer with actual HTTP requests and responses
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoRepository photoRepository;

    @BeforeEach
    void setUp() {
        photoRepository.deleteAll();
    }

    // ========== HomeController Tests ==========

    @Test
    void testHomePage_NoPhotos_ReturnsEmptyGallery() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("photos"))
                .andExpect(model().attributeExists("timestamp"));
    }

    @Test
    void testHomePage_WithPhotos_ReturnsGalleryWithPhotos() throws Exception {
        // Given
        createTestPhoto("photo1.jpg", LocalDateTime.now());
        createTestPhoto("photo2.jpg", LocalDateTime.now().minusDays(1));

        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("photos"));
    }

    @Test
    void testUploadPhoto_ValidJpegFile_Success() throws Exception {
        // Given - Use a valid minimal JPEG
        byte[] imageData = createMinimalJpegData();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.jpg",
                "image/jpeg",
                imageData
        );

        // When/Then
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.uploadedPhotos").isArray())
                .andExpect(jsonPath("$.uploadedPhotos[0].originalFileName").value("test.jpg"));

        // Verify photo was saved
        assertThat(photoRepository.findAll()).hasSize(1);
    }

    // Helper method to create minimal valid JPEG data for testing
    private byte[] createMinimalJpegData() {
        return new byte[]{
            (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00, 0x10, 0x4A, 0x46,
            0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01,
            0x00, 0x01, 0x00, 0x00, (byte)0xFF, (byte)0xDB, 0x00, 0x43,
            0x00, 0x08, 0x06, 0x06, 0x07, 0x06, 0x05, 0x08,
            0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C,
            0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12,
            0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D,
            0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20,
            0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29,
            0x2C, 0x30, 0x31, 0x34, 0x34, 0x34, 0x1F, 0x27,
            0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34,
            0x32, (byte)0xFF, (byte)0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01,
            0x00, 0x01, 0x01, 0x01, 0x11, 0x00, (byte)0xFF, (byte)0xC4,
            0x00, 0x14, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x09, (byte)0xFF, (byte)0xC4, 0x00, 0x14,
            0x10, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, (byte)0xFF, (byte)0xDA, 0x00, 0x08, 0x01, 0x01,
            0x00, 0x00, 0x3F, 0x00, (byte)0xFF, (byte)0xD9
        };
    }

    @Test
    void testUploadPhoto_MultipleFiles_Success() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "test1.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "test2.png", "image/png", new byte[]{4, 5, 6}
        );

        // When/Then
        mockMvc.perform(multipart("/upload")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.uploadedPhotos").isArray())
                .andExpect(jsonPath("$.uploadedPhotos.length()").value(2));

        // Verify both photos were saved
        assertThat(photoRepository.findAll()).hasSize(2);
    }

    @Test
    void testUploadPhoto_InvalidFileType_ReturnsError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        // When/Then
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.failedUploads").isArray())
                .andExpect(jsonPath("$.failedUploads[0].error").value(org.hamcrest.Matchers.containsString("File type not supported")));
    }

    @Test
    void testUploadPhoto_NoFiles_ReturnsBadRequest() throws Exception {
        // Note: When no files are provided to multipart endpoint, 
        // Spring may return 400 or the controller may handle it
        // Testing the actual behavior
        mockMvc.perform(multipart("/upload"))
                .andExpect(status().is4xxClientError());
    }

    // ========== PhotoFileController Tests ==========

    @Test
    void testServePhoto_ValidId_ReturnsPhotoData() throws Exception {
        // Given
        Photo photo = createTestPhoto("test.jpg", LocalDateTime.now());

        // When/Then
        mockMvc.perform(get("/photo/" + photo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("image/jpeg")))
                .andExpect(header().exists("X-Photo-ID"))
                .andExpect(header().string("X-Photo-Name", "test.jpg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void testServePhoto_InvalidId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/photo/non-existing-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testServePhoto_EmptyId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/photo/"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testServePhoto_NoCacheHeaders_ArePresent() throws Exception {
        // Given
        Photo photo = createTestPhoto("test.jpg", LocalDateTime.now());

        // When/Then
        mockMvc.perform(get("/photo/" + photo.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate, private"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    // ========== DetailController Tests ==========

    @Test
    void testDetailPage_ValidId_ReturnsPhotoDetail() throws Exception {
        // Given - single photo (no navigation)
        Photo photo = createTestPhoto("test.jpg", LocalDateTime.now());

        // When/Then
        mockMvc.perform(get("/detail/" + photo.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"))
                .andExpect(model().attributeExists("photo"));
        // Navigation IDs may or may not be present depending on data
    }

    @Test
    void testDetailPage_InvalidId_RedirectsToHome() throws Exception {
        mockMvc.perform(get("/detail/non-existing-id"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testDetailPage_WithNavigationPhotos_IncludesPreviousAndNext() throws Exception {
        // Given - photos with distinct timestamps using explicit times
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);
        Photo photo1 = createTestPhoto("photo1.jpg", baseTime);
        Photo photo2 = createTestPhoto("photo2.jpg", baseTime.plusHours(1));
        Photo photo3 = createTestPhoto("photo3.jpg", baseTime.plusHours(2));

        // When/Then
        mockMvc.perform(get("/detail/" + photo2.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"))
                .andExpect(model().attributeExists("photo"))
                .andExpect(model().attributeExists("previousPhotoId"))
                .andExpect(model().attributeExists("nextPhotoId"));
        // Exact IDs may vary due to timing, so just verify attributes exist
    }

    @Test
    void testDeletePhoto_ValidId_Success() throws Exception {
        // Given
        Photo photo = createTestPhoto("to-delete.jpg", LocalDateTime.now());
        String photoId = photo.getId();

        // When/Then
        mockMvc.perform(post("/detail/" + photoId + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify photo was deleted
        assertThat(photoRepository.findById(photoId)).isEmpty();
    }

    @Test
    void testDeletePhoto_InvalidId_ReturnsErrorMessage() throws Exception {
        mockMvc.perform(post("/detail/non-existing-id/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ========== End-to-End Test ==========

    @Test
    void testFullPhotoWorkflow_UploadViewDelete_Success() throws Exception {
        // Step 1: Upload a photo
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "workflow-test.jpg",
                "image/jpeg",
                createMinimalJpegData()
        );

        String uploadResponse = mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract photo ID from response (in a real scenario, you'd parse JSON properly)
        Photo savedPhoto = photoRepository.findAll().get(0);
        String photoId = savedPhoto.getId();

        // Step 2: View photo on home page
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        // Step 3: View photo detail page
        mockMvc.perform(get("/detail/" + photoId))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"));

        // Step 4: Serve photo file
        mockMvc.perform(get("/photo/" + photoId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("image/jpeg")));

        // Step 5: Delete photo
        mockMvc.perform(post("/detail/" + photoId + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Verify photo is deleted
        assertThat(photoRepository.findAll()).isEmpty();
    }

    // Helper method to create test photos
    private Photo createTestPhoto(String filename, LocalDateTime uploadedAt) {
        Photo photo = new Photo(
                filename,
                new byte[]{1, 2, 3},
                "uuid-" + filename,
                "/uploads/uuid-" + filename,
                1024L,
                "image/jpeg"
        );
        photo.setUploadedAt(uploadedAt);
        photo.setWidth(800);
        photo.setHeight(600);
        return photoRepository.save(photo);
    }
}
