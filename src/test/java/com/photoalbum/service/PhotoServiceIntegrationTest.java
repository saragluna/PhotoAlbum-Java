package com.photoalbum.service;

import com.photoalbum.model.Photo;
import com.photoalbum.model.UploadResult;
import com.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PhotoService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PhotoServiceIntegrationTest {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoRepository photoRepository;

    @Test
    public void testUploadPhotoSuccess() {
        // Create a mock image file (1x1 PNG)
        byte[] pngData = createTestPngImage();
        MultipartFile file = new MockMultipartFile(
            "test.png",
            "test.png",
            "image/png",
            pngData
        );

        // Upload the photo
        UploadResult result = photoService.uploadPhoto(file);

        // Verify the upload was successful
        assertTrue(result.isSuccess(), "Upload should be successful");
        assertNotNull(result.getPhotoId(), "Photo ID should not be null");
        assertNull(result.getErrorMessage(), "Error message should be null");

        // Verify the photo was saved to the database
        Optional<Photo> savedPhoto = photoService.getPhotoById(result.getPhotoId());
        assertTrue(savedPhoto.isPresent(), "Photo should be found in database");
        assertEquals("test.png", savedPhoto.get().getOriginalFileName());
        assertEquals("image/png", savedPhoto.get().getMimeType());
    }

    @Test
    public void testUploadPhotoInvalidMimeType() {
        // Create a mock file with invalid MIME type
        byte[] data = "test data".getBytes();
        MultipartFile file = new MockMultipartFile(
            "test.txt",
            "test.txt",
            "text/plain",
            data
        );

        // Upload the photo
        UploadResult result = photoService.uploadPhoto(file);

        // Verify the upload failed
        assertFalse(result.isSuccess(), "Upload should fail for invalid MIME type");
        assertNotNull(result.getErrorMessage(), "Error message should be present");
        assertTrue(result.getErrorMessage().contains("not supported"), 
            "Error message should mention unsupported file type");
    }

    @Test
    public void testUploadPhotoFileTooLarge() {
        // Create a large mock file (11MB, exceeds 10MB limit)
        byte[] largeData = new byte[11 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
            "large.png",
            "large.png",
            "image/png",
            largeData
        );

        // Upload the photo
        UploadResult result = photoService.uploadPhoto(file);

        // Verify the upload failed
        assertFalse(result.isSuccess(), "Upload should fail for large file");
        assertNotNull(result.getErrorMessage(), "Error message should be present");
        assertTrue(result.getErrorMessage().contains("exceeds"), 
            "Error message should mention size limit");
    }

    @Test
    public void testUploadPhotoEmptyFile() {
        // Create an empty mock file
        MultipartFile file = new MockMultipartFile(
            "empty.png",
            "empty.png",
            "image/png",
            new byte[0]
        );

        // Upload the photo
        UploadResult result = photoService.uploadPhoto(file);

        // Verify the upload failed
        assertFalse(result.isSuccess(), "Upload should fail for empty file");
        assertNotNull(result.getErrorMessage(), "Error message should be present");
        assertTrue(result.getErrorMessage().contains("empty"), 
            "Error message should mention empty file");
    }

    @Test
    public void testGetAllPhotos() {
        // Upload multiple photos
        for (int i = 0; i < 3; i++) {
            byte[] pngData = createTestPngImage();
            MultipartFile file = new MockMultipartFile(
                "test" + i + ".png",
                "test" + i + ".png",
                "image/png",
                pngData
            );
            photoService.uploadPhoto(file);
        }

        // Get all photos
        List<Photo> photos = photoService.getAllPhotos();

        // Verify we have at least 3 photos
        assertTrue(photos.size() >= 3, "Should have at least 3 photos");
    }

    @Test
    public void testGetPhotoById() {
        // Upload a photo
        byte[] pngData = createTestPngImage();
        MultipartFile file = new MockMultipartFile(
            "test.png",
            "test.png",
            "image/png",
            pngData
        );
        UploadResult result = photoService.uploadPhoto(file);

        // Get the photo by ID
        Optional<Photo> photo = photoService.getPhotoById(result.getPhotoId());

        // Verify the photo is found
        assertTrue(photo.isPresent(), "Photo should be found");
        assertEquals(result.getPhotoId(), photo.get().getId());
        assertEquals("test.png", photo.get().getOriginalFileName());
    }

    @Test
    public void testGetPhotoByIdNotFound() {
        // Try to get a non-existent photo
        Optional<Photo> photo = photoService.getPhotoById("non-existent-id");

        // Verify the photo is not found
        assertFalse(photo.isPresent(), "Photo should not be found");
    }

    @Test
    public void testDeletePhoto() {
        // Upload a photo
        byte[] pngData = createTestPngImage();
        MultipartFile file = new MockMultipartFile(
            "test.png",
            "test.png",
            "image/png",
            pngData
        );
        UploadResult result = photoService.uploadPhoto(file);
        String photoId = result.getPhotoId();

        // Verify the photo exists
        assertTrue(photoService.getPhotoById(photoId).isPresent(), 
            "Photo should exist before deletion");

        // Delete the photo
        boolean deleted = photoService.deletePhoto(photoId);

        // Verify deletion was successful
        assertTrue(deleted, "Delete should return true");
        assertFalse(photoService.getPhotoById(photoId).isPresent(), 
            "Photo should not exist after deletion");
    }

    @Test
    public void testDeletePhotoNotFound() {
        // Try to delete a non-existent photo
        boolean deleted = photoService.deletePhoto("non-existent-id");

        // Verify deletion failed
        assertFalse(deleted, "Delete should return false for non-existent photo");
    }

    @Test
    public void testGetPreviousAndNextPhoto() throws InterruptedException {
        // Upload three photos with small delays to ensure different timestamps
        UploadResult result1 = uploadTestPhoto("photo1.png");
        Thread.sleep(10);
        UploadResult result2 = uploadTestPhoto("photo2.png");
        Thread.sleep(10);
        UploadResult result3 = uploadTestPhoto("photo3.png");

        // Get the middle photo
        Optional<Photo> middlePhoto = photoService.getPhotoById(result2.getPhotoId());
        assertTrue(middlePhoto.isPresent(), "Middle photo should exist");

        // Get previous photo (should be photo1)
        Optional<Photo> previousPhoto = photoService.getPreviousPhoto(middlePhoto.get());
        assertTrue(previousPhoto.isPresent(), "Previous photo should exist");

        // Get next photo (should be photo3)
        Optional<Photo> nextPhoto = photoService.getNextPhoto(middlePhoto.get());
        assertTrue(nextPhoto.isPresent(), "Next photo should exist");
    }

    /**
     * Helper method to create a minimal valid PNG image
     */
    private byte[] createTestPngImage() {
        // This is a minimal 1x1 transparent PNG image
        return new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,  // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,  // 1x1 dimensions
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4, (byte)0x89,  // IHDR data
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,  // IDAT chunk
            0x78, (byte)0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,  // IDAT data
            0x0D, 0x0A, 0x2D, (byte)0xB4,  // IDAT CRC
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,  // IEND chunk
            (byte)0xAE, 0x42, 0x60, (byte)0x82  // IEND CRC
        };
    }

    /**
     * Helper method to upload a test photo
     */
    private UploadResult uploadTestPhoto(String filename) {
        byte[] pngData = createTestPngImage();
        MultipartFile file = new MockMultipartFile(
            filename,
            filename,
            "image/png",
            pngData
        );
        return photoService.uploadPhoto(file);
    }
}
