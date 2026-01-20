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
        // Given - Create a simple test image data (dimensions won't be extracted)
        byte[] imageData = "fake-jpeg-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            imageData
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
        byte[] imageData = "fake-jpeg-data".getBytes();
        
        MockMultipartFile file1 = new MockMultipartFile("file", "photo1.jpg", "image/jpeg", imageData);
        photoService.uploadPhoto(file1);
        
        Thread.sleep(100);
        
        MockMultipartFile file2 = new MockMultipartFile("file", "photo2.jpg", "image/jpeg", imageData);
        photoService.uploadPhoto(file2);
        
        Thread.sleep(100);
        
        MockMultipartFile file3 = new MockMultipartFile("file", "photo3.jpg", "image/jpeg", imageData);
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
        byte[] imageData = "fake-jpeg-data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", imageData);
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
        byte[] imageData = "fake-jpeg-data".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", imageData);
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
        byte[] imageData = "fake-jpeg-data".getBytes();
        
        MockMultipartFile file1 = new MockMultipartFile("file", "photo1.jpg", "image/jpeg", imageData);
        UploadResult result1 = photoService.uploadPhoto(file1);
        
        Thread.sleep(100);
        
        MockMultipartFile file2 = new MockMultipartFile("file", "photo2.jpg", "image/jpeg", imageData);
        UploadResult result2 = photoService.uploadPhoto(file2);
        
        Thread.sleep(100);
        
        MockMultipartFile file3 = new MockMultipartFile("file", "photo3.jpg", "image/jpeg", imageData);
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
        byte[] imageData = "fake-jpeg-data".getBytes();
        
        MockMultipartFile file1 = new MockMultipartFile("file", "photo1.jpg", "image/jpeg", imageData);
        UploadResult result1 = photoService.uploadPhoto(file1);
        
        Thread.sleep(100);
        
        MockMultipartFile file2 = new MockMultipartFile("file", "photo2.jpg", "image/jpeg", imageData);
        UploadResult result2 = photoService.uploadPhoto(file2);
        
        Thread.sleep(100);
        
        MockMultipartFile file3 = new MockMultipartFile("file", "photo3.jpg", "image/jpeg", imageData);
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
        byte[] imageData = "fake-jpeg-data".getBytes();
        byte[] pngData = "fake-png-data".getBytes();
        
        MockMultipartFile jpegFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", imageData);
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
}
