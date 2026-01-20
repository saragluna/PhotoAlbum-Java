package com.photoalbum;

import com.photoalbum.model.Photo;
import com.photoalbum.repository.PhotoRepository;
import com.photoalbum.service.PhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test for the complete photo album workflow
 * Tests the entire flow from upload to viewing to deletion
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PhotoAlbumEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoRepository photoRepository;

    @BeforeEach
    public void setup() {
        // Clean up before each test
        photoRepository.deleteAll();
    }

    @Test
    public void testCompletePhotoAlbumWorkflow() throws Exception {
        // Step 1: Verify the home page loads
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"));

        // Step 2: Upload a photo
        byte[] imageData = createTestPngImage();
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "vacation.png",
            "image/png",
            imageData
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.uploadedPhotos[0].id").exists())
            .andReturn();

        // Step 3: Verify the photo was saved to the database
        List<Photo> allPhotos = photoService.getAllPhotos();
        assertEquals(1, allPhotos.size(), "Should have 1 photo in database");
        Photo uploadedPhoto = allPhotos.get(0);
        assertEquals("vacation.png", uploadedPhoto.getOriginalFileName());
        assertNotNull(uploadedPhoto.getPhotoData(), "Photo data should be stored");
        assertEquals(imageData.length, uploadedPhoto.getPhotoData().length, 
            "Photo data size should match uploaded data");

        // Step 4: Retrieve and serve the photo
        mockMvc.perform(get("/photo/" + uploadedPhoto.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("image/png"))
            .andExpect(header().string("X-Photo-ID", uploadedPhoto.getId()))
            .andExpect(header().string("X-Photo-Name", "vacation.png"));

        // Step 5: View the photo detail page
        mockMvc.perform(get("/detail/" + uploadedPhoto.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("detail"))
            .andExpect(model().attributeExists("photo"));

        // Step 6: Delete the photo
        mockMvc.perform(post("/detail/" + uploadedPhoto.getId() + "/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        // Step 7: Verify the photo was deleted
        allPhotos = photoService.getAllPhotos();
        assertEquals(0, allPhotos.size(), "Should have 0 photos after deletion");
    }

    @Test
    public void testMultiplePhotoUploadAndNavigation() throws Exception {
        // Upload three photos with small delays
        byte[] imageData = createTestPngImage();
        
        MockMultipartFile file1 = new MockMultipartFile("files", "photo1.png", "image/png", imageData);
        mockMvc.perform(multipart("/upload").file(file1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
        Thread.sleep(10);

        MockMultipartFile file2 = new MockMultipartFile("files", "photo2.png", "image/png", imageData);
        mockMvc.perform(multipart("/upload").file(file2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
        Thread.sleep(10);

        MockMultipartFile file3 = new MockMultipartFile("files", "photo3.png", "image/png", imageData);
        mockMvc.perform(multipart("/upload").file(file3))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify all three photos are in the database
        List<Photo> allPhotos = photoService.getAllPhotos();
        assertEquals(3, allPhotos.size(), "Should have 3 photos");

        // Get the middle photo
        Photo middlePhoto = allPhotos.get(1);

        // View detail page for middle photo
        mockMvc.perform(get("/detail/" + middlePhoto.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("detail"))
            .andExpect(model().attributeExists("photo"))
            .andExpect(model().attributeExists("previousPhotoId"))
            .andExpect(model().attributeExists("nextPhotoId"));

        // Verify navigation is available
        assertTrue(photoService.getPreviousPhoto(middlePhoto).isPresent(), 
            "Should have a previous photo");
        assertTrue(photoService.getNextPhoto(middlePhoto).isPresent(), 
            "Should have a next photo");
    }

    @Test
    public void testPhotoUploadValidation() throws Exception {
        // Test 1: Upload with invalid MIME type
        MockMultipartFile textFile = new MockMultipartFile(
            "files",
            "document.txt",
            "text/plain",
            "not an image".getBytes()
        );

        mockMvc.perform(multipart("/upload").file(textFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.failedUploads[0].error").exists());

        // Test 2: Upload empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
            "files",
            "empty.png",
            "image/png",
            new byte[0]
        );

        mockMvc.perform(multipart("/upload").file(emptyFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.failedUploads[0].error").exists());

        // Verify no photos were saved
        List<Photo> allPhotos = photoService.getAllPhotos();
        assertEquals(0, allPhotos.size(), "Should have 0 photos after validation failures");
    }

    @Test
    public void testBatchPhotoUploadAndDeletion() throws Exception {
        // Upload 5 photos
        byte[] imageData = createTestPngImage();
        for (int i = 0; i < 5; i++) {
            MockMultipartFile file = new MockMultipartFile(
                "files",
                "photo" + i + ".png",
                "image/png",
                imageData
            );
            mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        // Verify 5 photos were uploaded
        List<Photo> allPhotos = photoService.getAllPhotos();
        assertEquals(5, allPhotos.size(), "Should have 5 photos");

        // Delete 3 photos
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/detail/" + allPhotos.get(i).getId() + "/delete"))
                .andExpect(status().is3xxRedirection());
        }

        // Verify 2 photos remain
        allPhotos = photoService.getAllPhotos();
        assertEquals(2, allPhotos.size(), "Should have 2 photos after deletion");
    }

    @Test
    public void testPhotoDataIntegrity() throws Exception {
        // Upload a photo
        byte[] originalImageData = createTestPngImage();
        MockMultipartFile file = new MockMultipartFile(
            "files",
            "integrity-test.png",
            "image/png",
            originalImageData
        );

        mockMvc.perform(multipart("/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Retrieve the photo from database
        List<Photo> photos = photoService.getAllPhotos();
        assertEquals(1, photos.size());
        Photo photo = photos.get(0);

        // Verify photo data integrity
        assertNotNull(photo.getPhotoData(), "Photo data should not be null");
        assertArrayEquals(originalImageData, photo.getPhotoData(), 
            "Retrieved photo data should match original");

        // Serve the photo and verify data integrity
        MvcResult result = mockMvc.perform(get("/photo/" + photo.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("image/png"))
            .andReturn();

        byte[] servedImageData = result.getResponse().getContentAsByteArray();
        assertArrayEquals(originalImageData, servedImageData, 
            "Served photo data should match original");
    }

    @Test
    public void testErrorHandling() throws Exception {
        // Test 1: View detail page for non-existent photo - should redirect
        mockMvc.perform(get("/detail/non-existent-id"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        // Test 2: Delete non-existent photo - should redirect
        mockMvc.perform(post("/detail/non-existent-id/delete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        // Test 3: Serve non-existent photo
        mockMvc.perform(get("/photo/non-existent-id"))
            .andExpect(status().isNotFound());

        // Test 4: Upload with no files - should return bad request
        mockMvc.perform(multipart("/upload"))
            .andExpect(status().isBadRequest());
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
