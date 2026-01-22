package com.photoalbum.controller;

import com.photoalbum.AbstractIntegrationTest;
import com.photoalbum.model.Photo;
import com.photoalbum.repository.PhotoRepository;
import com.photoalbum.service.PhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for REST API endpoints using Testcontainers PostgreSQL
 */
@AutoConfigureMockMvc
class PhotoControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoRepository photoRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        photoRepository.deleteAll();
    }

    @Test
    void testGetHomePage() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("photos"))
                .andExpect(model().attributeExists("timestamp"));
    }

    @Test
    void testUploadPhotoSuccess() throws Exception {
        // Given
        byte[] imageData = "fake-jpeg-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "test-image.jpg",
            "image/jpeg",
            imageData
        );

        // When & Then
        mockMvc.perform(multipart("/upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedPhotos").isArray())
                .andExpect(jsonPath("$.uploadedPhotos[0].originalFileName").value("test-image.jpg"))
                .andExpect(jsonPath("$.uploadedPhotos[0].id").exists());
    }

    @Test
    void testUploadMultiplePhotos() throws Exception {
        // Given
        byte[] imageData = "fake-jpeg-data".getBytes();
        MockMultipartFile file1 = new MockMultipartFile("files", "photo1.jpg", "image/jpeg", imageData);
        MockMultipartFile file2 = new MockMultipartFile("files", "photo2.jpg", "image/jpeg", imageData);

        // When & Then
        mockMvc.perform(multipart("/upload")
                .file(file1)
                .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedPhotos").isArray())
                .andExpect(jsonPath("$.uploadedPhotos", hasSize(2)));
    }

    @Test
    void testUploadPhotoInvalidFileType() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "test.txt",
            "text/plain",
            "This is not an image".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.uploadedPhotos").isEmpty())
                .andExpect(jsonPath("$.failedUploads").isArray())
                .andExpect(jsonPath("$.failedUploads", hasSize(1)))
                .andExpect(jsonPath("$.failedUploads[0].error").value(containsString("File type not supported")));
    }

    @Test
    void testUploadPhotoNoFiles() throws Exception {
        // When & Then - missing the 'files' parameter causes bad request
        // Spring will return 400 if required @RequestParam is missing
        mockMvc.perform(multipart("/upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testServePhoto() throws Exception {
        // Given
        byte[] photoData = "test photo data".getBytes();
        Photo photo = new Photo("test.jpg", photoData, "stored-test.jpg", 
                               "/uploads/test.jpg", 1024L, "image/jpeg");
        Photo savedPhoto = photoRepository.save(photo);

        // When & Then
        mockMvc.perform(get("/photo/" + savedPhoto.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
                .andExpect(header().exists("X-Photo-ID"))
                .andExpect(header().string("X-Photo-Name", "test.jpg"))
                .andExpect(content().bytes(photoData));
    }

    @Test
    void testServePhotoNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/photo/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDetailPage() throws Exception {
        // Given
        byte[] photoData = "test photo data".getBytes();
        Photo photo = new Photo("test.jpg", photoData, "stored-test.jpg", 
                               "/uploads/test.jpg", 1024L, "image/jpeg");
        Photo savedPhoto = photoRepository.save(photo);

        // When & Then
        mockMvc.perform(get("/detail/" + savedPhoto.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"))
                .andExpect(model().attributeExists("photo"))
                .andExpect(model().attribute("photo", hasProperty("id", equalTo(savedPhoto.getId()))))
                .andExpect(model().attribute("photo", hasProperty("originalFileName", equalTo("test.jpg"))));
    }

    @Test
    void testDetailPageNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/detail/non-existent-id"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testDeletePhoto() throws Exception {
        // Given
        byte[] photoData = "test photo data".getBytes();
        Photo photo = new Photo("test.jpg", photoData, "stored-test.jpg", 
                               "/uploads/test.jpg", 1024L, "image/jpeg");
        Photo savedPhoto = photoRepository.save(photo);

        // When & Then
        mockMvc.perform(post("/detail/" + savedPhoto.getId() + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testDeletePhotoNotFound() throws Exception {
        // When & Then
        mockMvc.perform(post("/detail/non-existent-id/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testPhotoNavigationWithMultiplePhotos() throws Exception {
        // Given
        byte[] photoData = "test photo data".getBytes();
        Photo photo1 = new Photo("photo1.jpg", photoData, "stored-1.jpg", 
                                "/uploads/photo1.jpg", 1024L, "image/jpeg");
        photoRepository.save(photo1);
        
        Thread.sleep(100);
        
        Photo photo2 = new Photo("photo2.jpg", photoData, "stored-2.jpg", 
                                "/uploads/photo2.jpg", 2048L, "image/png");
        Photo savedPhoto2 = photoRepository.save(photo2);
        
        Thread.sleep(100);
        
        Photo photo3 = new Photo("photo3.jpg", photoData, "stored-3.jpg", 
                                "/uploads/photo3.jpg", 3072L, "image/gif");
        photoRepository.save(photo3);

        // When & Then - Check middle photo has both previous and next
        mockMvc.perform(get("/detail/" + savedPhoto2.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"))
                .andExpect(model().attributeExists("previousPhotoId"))
                .andExpect(model().attributeExists("nextPhotoId"))
                .andExpect(model().attribute("previousPhotoId", notNullValue()))
                .andExpect(model().attribute("nextPhotoId", notNullValue()));
    }

    @Test
    void testUploadAndRetrievePhotoEndToEnd() throws Exception {
        // Given
        byte[] imageData = "fake-jpeg-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "end-to-end-test.jpg",
            "image/jpeg",
            imageData
        );

        // Step 1: Upload photo
        String response = mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify upload was successful in response
        if (response.contains("\"uploadedPhotos\"")) {
            // Extract photo ID from response
            String photoId = response.substring(
                response.indexOf("\"id\":\"") + 6,
                response.indexOf("\"", response.indexOf("\"id\":\"") + 6)
            );

            // Step 2: Retrieve photo via API
            mockMvc.perform(get("/photo/" + photoId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

            // Step 3: View detail page
            mockMvc.perform(get("/detail/" + photoId))
                    .andExpect(status().isOk())
                    .andExpect(view().name("detail"))
                    .andExpect(model().attribute("photo", hasProperty("originalFileName", equalTo("end-to-end-test.jpg"))));

            // Step 4: Delete photo
            mockMvc.perform(post("/detail/" + photoId + "/delete"))
                    .andExpect(status().is3xxRedirection());

            // Step 5: Verify photo is deleted
            mockMvc.perform(get("/photo/" + photoId))
                    .andExpect(status().isNotFound());
        }
    }
}
