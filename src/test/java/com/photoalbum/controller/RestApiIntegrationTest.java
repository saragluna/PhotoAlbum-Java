package com.photoalbum.controller;

import com.photoalbum.model.Photo;
import com.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for REST API endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RestApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoRepository photoRepository;

    @BeforeEach
    public void setup() {
        // Clean up before each test
        photoRepository.deleteAll();
    }

    @Test
    public void testHomePageLoads() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"));
    }

    @Test
    public void testUploadPhotoSuccess() throws Exception {
        // Create a test image file
        byte[] imageData = createTestPngImage();
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "test.png",
            "image/png",
            imageData
        );

        // Upload the photo
        mockMvc.perform(multipart("/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.uploadedPhotos").isArray())
            .andExpect(jsonPath("$.uploadedPhotos[0].originalFileName").value("test.png"))
            .andExpect(jsonPath("$.uploadedPhotos[0].id").exists());
    }

    @Test
    public void testUploadMultiplePhotos() throws Exception {
        // Create multiple test image files
        byte[] imageData = createTestPngImage();
        MockMultipartFile file1 = new MockMultipartFile(
            "files",
            "test1.png",
            "image/png",
            imageData
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "files",
            "test2.png",
            "image/png",
            imageData
        );

        // Upload multiple photos
        mockMvc.perform(multipart("/upload")
                .file(file1)
                .file(file2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.uploadedPhotos").isArray())
            .andExpect(jsonPath("$.uploadedPhotos", hasSize(2)));
    }

    @Test
    public void testUploadPhotoInvalidMimeType() throws Exception {
        // Create a test file with invalid MIME type
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        // Upload the file
        mockMvc.perform(multipart("/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.failedUploads").isArray())
            .andExpect(jsonPath("$.failedUploads[0].error").value(containsString("not supported")));
    }

    @Test
    public void testUploadPhotoNoFiles() throws Exception {
        // Try to upload without files parameter - this will result in MissingServletRequestPartException
        // which Spring Boot handles by returning 400
        mockMvc.perform(multipart("/upload"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testServePhoto() throws Exception {
        // Create and save a photo
        Photo photo = createTestPhoto("test.png", createTestPngImage());
        Photo savedPhoto = photoRepository.save(photo);

        // Request the photo
        mockMvc.perform(get("/photo/" + savedPhoto.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("image/png"))
            .andExpect(header().string("X-Photo-ID", savedPhoto.getId()))
            .andExpect(header().string("X-Photo-Name", "test.png"))
            .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    public void testServePhotoNotFound() throws Exception {
        // Request a non-existent photo
        mockMvc.perform(get("/photo/non-existent-id"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testServePhotoEmptyId() throws Exception {
        // Request with empty ID
        mockMvc.perform(get("/photo/ "))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testDetailPage() throws Exception {
        // Create and save a photo
        Photo photo = createTestPhoto("test.png", createTestPngImage());
        Photo savedPhoto = photoRepository.save(photo);

        // Request the detail page
        mockMvc.perform(get("/detail/" + savedPhoto.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("detail"))
            .andExpect(model().attributeExists("photo"));
    }

    @Test
    public void testDetailPageNotFound() throws Exception {
        // Request detail page for non-existent photo - expect redirect to home
        mockMvc.perform(get("/detail/non-existent-id"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    public void testDeletePhoto() throws Exception {
        // Create and save a photo
        Photo photo = createTestPhoto("test.png", createTestPngImage());
        Photo savedPhoto = photoRepository.save(photo);

        // Delete the photo via detail page endpoint
        mockMvc.perform(post("/detail/" + savedPhoto.getId() + "/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        // Verify the photo was deleted
        assert(!photoRepository.findById(savedPhoto.getId()).isPresent());
    }

    @Test
    public void testDeletePhotoNotFound() throws Exception {
        // Try to delete a non-existent photo - should still redirect
        mockMvc.perform(post("/detail/non-existent-id/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    /**
     * Helper method to create a test photo
     */
    private Photo createTestPhoto(String filename, byte[] photoData) {
        return new Photo(
            filename,
            photoData,
            filename,
            "/uploads/" + filename,
            (long) photoData.length,
            "image/png"
        );
    }

    /**
     * Helper method to create a minimal valid PNG image
     */
    private byte[] createTestPngImage() {
        // This is a minimal 1x1 transparent PNG image
        return new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4, (byte)0x89,
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
            0x78, (byte)0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
            0x0D, 0x0A, 0x2D, (byte)0xB4,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte)0xAE, 0x42, 0x60, (byte)0x82
        };
    }
}
