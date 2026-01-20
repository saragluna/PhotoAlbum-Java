package com.photoalbum.service;

import com.photoalbum.model.Photo;
import com.photoalbum.model.UploadResult;
import com.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PhotoService
 * Tests the business logic layer with actual database interactions
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PhotoServiceIntegrationTest {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private PhotoRepository photoRepository;

    @BeforeEach
    void setUp() {
        photoRepository.deleteAll();
    }

    @Test
    void testUploadPhoto_ValidJpegImage_Success() {
        // Given - Create a minimal valid JPEG file
        byte[] imageData = createMinimalJpegData();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                imageData
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPhotoId()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();

        // Verify photo was saved to database
        Optional<Photo> savedPhoto = photoRepository.findById(result.getPhotoId());
        assertThat(savedPhoto).isPresent();
        assertThat(savedPhoto.get().getOriginalFileName()).isEqualTo("test.jpg");
        assertThat(savedPhoto.get().getMimeType()).isEqualTo("image/jpeg");
    }

    // Helper method to create minimal valid JPEG data for testing
    private byte[] createMinimalJpegData() {
        // Create a minimal valid JPEG file (1x1 pixel black image)
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
    void testUploadPhoto_ValidPngImage_Success() {
        // Given - Use a minimal valid PNG file
        byte[] imageData = createMinimalPngData();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                imageData
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPhotoId()).isNotNull();
    }

    // Helper method to create minimal valid PNG data for testing
    private byte[] createMinimalPngData() {
        // Create a minimal valid PNG file (1x1 pixel transparent image)
        return new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
            (byte)0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte)0x9C, 0x63, 0x00, 0x01, 0x00,
            0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte)0xB4,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte)0xAE, 0x42, 0x60, (byte)0x82
        };
    }

    @Test
    void testUploadPhoto_InvalidMimeType_Failure() {
        // Given
        byte[] fileData = new byte[]{1, 2, 3};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                fileData
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("File type not supported");
    }

    @Test
    void testUploadPhoto_EmptyFile_Failure() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[]{}
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("File is empty");
    }

    @Test
    void testUploadPhoto_FileTooLarge_Failure() {
        // Given - Create file larger than max size (10MB)
        byte[] largeData = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeData
        );

        // When
        UploadResult result = photoService.uploadPhoto(file);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("exceeds");
    }

    @Test
    void testGetAllPhotos_ReturnsPhotosInDescendingOrder() {
        // Given - Create multiple photos with different timestamps
        createTestPhoto("photo1.jpg", LocalDateTime.now().minusDays(3));
        createTestPhoto("photo2.jpg", LocalDateTime.now().minusDays(2));
        createTestPhoto("photo3.jpg", LocalDateTime.now().minusDays(1));

        // When
        List<Photo> photos = photoService.getAllPhotos();

        // Then
        assertThat(photos).hasSize(3);
        assertThat(photos.get(0).getOriginalFileName()).isEqualTo("photo3.jpg");
        assertThat(photos.get(1).getOriginalFileName()).isEqualTo("photo2.jpg");
        assertThat(photos.get(2).getOriginalFileName()).isEqualTo("photo1.jpg");
    }

    @Test
    void testGetPhotoById_ExistingPhoto_ReturnsPhoto() {
        // Given
        Photo photo = createTestPhoto("test.jpg", LocalDateTime.now());

        // When
        Optional<Photo> result = photoService.getPhotoById(photo.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOriginalFileName()).isEqualTo("test.jpg");
    }

    @Test
    void testGetPhotoById_NonExistingPhoto_ReturnsEmpty() {
        // When
        Optional<Photo> result = photoService.getPhotoById("non-existing-id");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testDeletePhoto_ExistingPhoto_Success() {
        // Given
        Photo photo = createTestPhoto("to-delete.jpg", LocalDateTime.now());
        String photoId = photo.getId();

        // When
        boolean deleted = photoService.deletePhoto(photoId);

        // Then
        assertThat(deleted).isTrue();
        assertThat(photoRepository.findById(photoId)).isEmpty();
    }

    @Test
    void testDeletePhoto_NonExistingPhoto_ReturnsFalse() {
        // When
        boolean deleted = photoService.deletePhoto("non-existing-id");

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void testGetPreviousPhoto_ReturnsOlderPhoto() {
        // Given
        photoRepository.deleteAll();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);
        Photo oldPhoto = createTestPhoto("old.jpg", baseTime);
        Photo currentPhoto = createTestPhoto("current.jpg", baseTime.plusHours(1));
        Photo newPhoto = createTestPhoto("new.jpg", baseTime.plusHours(2));

        // When
        Optional<Photo> previous = photoService.getPreviousPhoto(currentPhoto);

        // Then
        assertThat(previous).isPresent();
        assertThat(previous.get().getUploadedAt()).isBefore(currentPhoto.getUploadedAt());
    }

    @Test
    void testGetPreviousPhoto_NoOlderPhotos_ReturnsEmpty() {
        // Given - Clear all data and create just one photo  
        photoRepository.deleteAll();
        Photo oldestPhoto = createTestPhoto("oldest.jpg", LocalDateTime.now().minusDays(10));

        // When
        Optional<Photo> previous = photoService.getPreviousPhoto(oldestPhoto);

        // Then
        assertThat(previous).isEmpty();
    }

    @Test
    void testGetNextPhoto_ReturnsNewerPhoto() {
        // Given
        photoRepository.deleteAll();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);
        Photo oldPhoto = createTestPhoto("old.jpg", baseTime);
        Photo currentPhoto = createTestPhoto("current.jpg", baseTime.plusHours(1));  
        Photo newPhoto = createTestPhoto("new.jpg", baseTime.plusHours(2));

        // When
        Optional<Photo> next = photoService.getNextPhoto(currentPhoto);

        // Then
        assertThat(next).isPresent();
        assertThat(next.get().getUploadedAt()).isAfter(currentPhoto.getUploadedAt());
    }

    @Test
    void testGetNextPhoto_NoNewerPhotos_ReturnsEmpty() {
        // Given - Only one photo
        Photo photo = createTestPhoto("only.jpg", LocalDateTime.now());

        // When
        Optional<Photo> next = photoService.getNextPhoto(photo);

        // Then
        assertThat(next).isEmpty();
    }

    @Test
    void testPhotoNavigation_MultiplePhotos_WorksCorrectly() {
        // Given - Create a sequence of photos with distinct timestamps
        photoRepository.deleteAll();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(10);
        Photo photo1 = createTestPhoto("photo1.jpg", baseTime);
        Photo photo2 = createTestPhoto("photo2.jpg", baseTime.plusHours(1));
        Photo photo3 = createTestPhoto("photo3.jpg", baseTime.plusHours(2));

        // When/Then - Navigate from middle photo
        Optional<Photo> previous = photoService.getPreviousPhoto(photo2);
        Optional<Photo> next = photoService.getNextPhoto(photo2);

        // Verify navigation works
        assertThat(previous).isPresent();
        assertThat(previous.get().getUploadedAt()).isBefore(photo2.getUploadedAt());

        assertThat(next).isPresent();
        assertThat(next.get().getUploadedAt()).isAfter(photo2.getUploadedAt());
    }

    // Helper method to create test photos
    private Photo createTestPhoto(String filename, LocalDateTime uploadedAt) {
        Photo photo = new Photo(filename, new byte[]{1, 2, 3}, "uuid-" + filename, "/uploads/uuid-" + filename, 1024L, "image/jpeg");
        photo.setUploadedAt(uploadedAt);
        photo.setWidth(800);
        photo.setHeight(600);
        return photoRepository.save(photo);
    }
}
