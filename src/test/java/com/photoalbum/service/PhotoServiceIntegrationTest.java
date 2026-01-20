package com.photoalbum.service;

import com.photoalbum.AbstractIntegrationTest;
import com.photoalbum.model.Photo;
import com.photoalbum.model.UploadResult;
import com.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PhotoService using Testcontainers PostgreSQL
 */
class PhotoServiceIntegrationTest extends AbstractIntegrationTest {

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
    void testUploadPhotoSuccess() {
        // Given - Create a simple JPEG image (minimal valid JPEG header)
        byte[] jpegData = createMinimalJpegData();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            jpegData
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getPhotoId());
        assertNull(result.getErrorMessage());
        
        // Verify photo is saved in database
        Optional<Photo> savedPhoto = photoService.getPhotoById(result.getPhotoId());
        assertTrue(savedPhoto.isPresent());
        assertEquals("test-image.jpg", savedPhoto.get().getOriginalFileName());
        assertEquals("image/jpeg", savedPhoto.get().getMimeType());
    }

    @Test
    void testUploadPhotoInvalidFileType() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "This is not an image".getBytes()
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("File type not supported"));
    }

    @Test
    void testUploadPhotoFileTooLarge() {
        // Given - Create a file larger than 10MB
        byte[] largeData = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large-image.jpg",
            "image/jpeg",
            largeData
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("exceeds"));
    }

    @Test
    void testUploadPhotoEmptyFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("empty"));
    }

    @Test
    void testGetAllPhotos() throws InterruptedException {
        // Given
        byte[] jpegData = createMinimalJpegData();
        
        MockMultipartFile file1 = new MockMultipartFile("file", "photo1.jpg", "image/jpeg", jpegData);
        photoService.uploadPhoto(file1);
        
        Thread.sleep(100);
        
        MockMultipartFile file2 = new MockMultipartFile("file", "photo2.jpg", "image/jpeg", jpegData);
        photoService.uploadPhoto(file2);
        
        Thread.sleep(100);
        
        MockMultipartFile file3 = new MockMultipartFile("file", "photo3.jpg", "image/jpeg", jpegData);
        photoService.uploadPhoto(file3);

        // When
        List<Photo> photos = photoService.getAllPhotos();

        // Then
        assertEquals(3, photos.size());
        // Verify they're in descending order (newest first)
        assertEquals("photo3.jpg", photos.get(0).getOriginalFileName());
        assertEquals("photo2.jpg", photos.get(1).getOriginalFileName());
        assertEquals("photo1.jpg", photos.get(2).getOriginalFileName());
    }

    @Test
    void testGetPhotoById() {
        // Given
        byte[] jpegData = createMinimalJpegData();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegData);
        UploadResult uploadResult = photoService.uploadPhoto(file);

        // When
        Optional<Photo> photo = photoService.getPhotoById(uploadResult.getPhotoId());

        // Then
        assertTrue(photo.isPresent());
        assertEquals("test.jpg", photo.get().getOriginalFileName());
        assertNotNull(photo.get().getPhotoData());
    }

    @Test
    void testGetPhotoByIdNotFound() {
        // When
        Optional<Photo> photo = photoService.getPhotoById("non-existent-id");

        // Then
        assertFalse(photo.isPresent());
    }

    @Test
    void testDeletePhoto() {
        // Given
        byte[] jpegData = createMinimalJpegData();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegData);
        UploadResult uploadResult = photoService.uploadPhoto(file);
        String photoId = uploadResult.getPhotoId();

        // When
        boolean deleted = photoService.deletePhoto(photoId);

        // Then
        assertTrue(deleted);
        
        // Verify photo is deleted
        Optional<Photo> deletedPhoto = photoService.getPhotoById(photoId);
        assertFalse(deletedPhoto.isPresent());
    }

    @Test
    void testDeletePhotoNotFound() {
        // When
        boolean deleted = photoService.deletePhoto("non-existent-id");

        // Then
        assertFalse(deleted);
    }

    @Test
    void testGetPreviousPhoto() throws InterruptedException {
        // Given
        byte[] jpegData = createMinimalJpegData();
        
        MockMultipartFile file1 = new MockMultipartFile("file", "photo1.jpg", "image/jpeg", jpegData);
        UploadResult result1 = photoService.uploadPhoto(file1);
        
        Thread.sleep(100);
        
        MockMultipartFile file2 = new MockMultipartFile("file", "photo2.jpg", "image/jpeg", jpegData);
        UploadResult result2 = photoService.uploadPhoto(file2);
        
        Thread.sleep(100);
        
        MockMultipartFile file3 = new MockMultipartFile("file", "photo3.jpg", "image/jpeg", jpegData);
        UploadResult result3 = photoService.uploadPhoto(file3);

        Photo currentPhoto = photoService.getPhotoById(result2.getPhotoId()).get();

        // When
        Optional<Photo> previousPhoto = photoService.getPreviousPhoto(currentPhoto);

        // Then
        assertTrue(previousPhoto.isPresent());
        assertEquals("photo1.jpg", previousPhoto.get().getOriginalFileName());
    }

    @Test
    void testGetNextPhoto() throws InterruptedException {
        // Given
        byte[] jpegData = createMinimalJpegData();
        
        MockMultipartFile file1 = new MockMultipartFile("file", "photo1.jpg", "image/jpeg", jpegData);
        UploadResult result1 = photoService.uploadPhoto(file1);
        
        Thread.sleep(100);
        
        MockMultipartFile file2 = new MockMultipartFile("file", "photo2.jpg", "image/jpeg", jpegData);
        UploadResult result2 = photoService.uploadPhoto(file2);
        
        Thread.sleep(100);
        
        MockMultipartFile file3 = new MockMultipartFile("file", "photo3.jpg", "image/jpeg", jpegData);
        UploadResult result3 = photoService.uploadPhoto(file3);

        Photo currentPhoto = photoService.getPhotoById(result2.getPhotoId()).get();

        // When
        Optional<Photo> nextPhoto = photoService.getNextPhoto(currentPhoto);

        // Then
        assertTrue(nextPhoto.isPresent());
        assertEquals("photo3.jpg", nextPhoto.get().getOriginalFileName());
    }

    @Test
    void testUploadMultipleDifferentFileTypes() {
        // Given
        byte[] jpegData = createMinimalJpegData();
        byte[] pngData = createMinimalPngData();
        
        MockMultipartFile jpegFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegData);
        MockMultipartFile pngFile = new MockMultipartFile("file", "test.png", "image/png", pngData);

        // When
        UploadResult jpegResult = photoService.uploadPhoto(jpegFile);
        UploadResult pngResult = photoService.uploadPhoto(pngFile);

        // Then
        assertTrue(jpegResult.isSuccess());
        assertTrue(pngResult.isSuccess());
        
        List<Photo> allPhotos = photoService.getAllPhotos();
        assertEquals(2, allPhotos.size());
    }

    /**
     * Helper method to create minimal valid JPEG data
     */
    private byte[] createMinimalJpegData() {
        // Minimal JPEG structure: SOI + APP0 + SOF + SOS + EOI
        return new byte[] {
            (byte) 0xFF, (byte) 0xD8, // SOI (Start of Image)
            (byte) 0xFF, (byte) 0xE0, // APP0
            0x00, 0x10, // Length
            0x4A, 0x46, 0x49, 0x46, 0x00, // JFIF\0
            0x01, 0x01, // Version
            0x00, // Units
            0x00, 0x01, 0x00, 0x01, // X/Y density
            0x00, 0x00, // Thumbnail size
            (byte) 0xFF, (byte) 0xD9 // EOI (End of Image)
        };
    }

    /**
     * Helper method to create minimal valid PNG data
     */
    private byte[] createMinimalPngData() {
        // Minimal PNG structure: signature + IHDR + IEND
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR length
            0x49, 0x48, 0x44, 0x52, // IHDR
            0x00, 0x00, 0x00, 0x01, // Width
            0x00, 0x00, 0x00, 0x01, // Height
            0x08, 0x06, 0x00, 0x00, 0x00, // Bit depth, color type, etc.
            0x1F, 0x15, (byte) 0xC4, (byte) 0x89, // CRC
            0x00, 0x00, 0x00, 0x00, // IEND length
            0x49, 0x45, 0x4E, 0x44, // IEND
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82 // CRC
        };
    }
}
