package org.example.authentication_cookiesession.repository;

import org.example.authentication_cookiesession.PostgresTestcontainersConfiguration;
import org.example.authentication_cookiesession.entity.Admin;
import org.example.authentication_cookiesession.entity.Image;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for Spring Data JPA repository queries against PostgreSQL.
 *
 * <p>The repositories are the system under test. EntityManager and JdbcTemplate
 * are test fixtures used only to control persistence context and test data.</p>
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainersConfiguration.class)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/migration/V1_create_schemas.sql"
})
class RepositoryDataJpaTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Verifies that the custom native insert stores the supplied UUID and URL.
     */
    @Test
    void insertUploadedImagePersistsImageRecord() {
        UUID imageId = UUID.randomUUID();
        String imageUrl = imageUrl(imageId);

        imageRepository.insertUploadedImage(imageId, imageUrl);
        entityManager.clear();

        Image persistedImage = imageRepository.findById(imageId).orElseThrow();

        assertEquals(imageId, persistedImage.getId());
        assertEquals(imageUrl, persistedImage.getImageUrl());
        assertNotNull(persistedImage.getCreatedAt());
    }

    /**
     * Verifies that the custom delete removes exactly one matching image row.
     */
    @Test
    void deleteImageByIdRemovesMatchingImageRecord() {
        UUID imageId = UUID.randomUUID();
        imageRepository.insertUploadedImage(imageId, imageUrl(imageId));

        int deletedRows = imageRepository.deleteImageById(imageId);
        entityManager.clear();

        assertEquals(1, deletedRows);
        assertTrue(imageRepository.findById(imageId).isEmpty());
    }

    /**
     * Verifies that deleting a missing image does not report a false deletion.
     */
    @Test
    void deleteImageByIdReturnsZeroWhenImageDoesNotExist() {
        int deletedRows = imageRepository.deleteImageById(UUID.randomUUID());

        assertEquals(0, deletedRows);
    }

    /**
     * Verifies newest-first ordering and the UUID tie-breaker in the custom query.
     */
    @Test
    void findImageUrlsOrderByCreatedAtDescUsesCreatedAtThenId() {
        OffsetDateTime baseTime = OffsetDateTime.now().withNano(0);
        UUID oldestId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID tiedLowerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID tiedHigherId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID newestId = UUID.fromString("00000000-0000-0000-0000-000000000004");

        insertImageWithCreatedAt(oldestId, baseTime.minusDays(1));
        insertImageWithCreatedAt(tiedLowerId, baseTime);
        insertImageWithCreatedAt(tiedHigherId, baseTime);
        insertImageWithCreatedAt(newestId, baseTime.plusDays(1));

        List<String> imageUrls = imageRepository.findImageUrlsOrderByCreatedAtDesc();

        assertEquals(
                List.of(
                        imageUrl(newestId),
                        imageUrl(tiedHigherId),
                        imageUrl(tiedLowerId),
                        imageUrl(oldestId)),
                imageUrls);
    }

    /**
     * Verifies that the derived admin email query returns the matching account.
     */
    @Test
    void findByEmailReturnsMatchingAdmin() {
        Admin admin = Admin.create("admin@example.com", "bcrypt-hash");
        adminRepository.saveAndFlush(admin);
        entityManager.clear();

        Optional<Admin> result = adminRepository.findByEmail(admin.getEmail());

        assertTrue(result.isPresent());
        assertEquals(admin.getEmail(), result.get().getEmail());
        assertEquals(admin.getPasswordHash(), result.get().getPasswordHash());
    }

    /**
     * Verifies that an unknown email produces an empty Optional.
     */
    @Test
    void findByEmailReturnsEmptyWhenAdminDoesNotExist() {
        Optional<Admin> result = adminRepository.findByEmail("missing@example.com");

        assertTrue(result.isEmpty());
    }

    /**
     * Inserts deterministic image data without making the repository query the system under test.
     */
    private void insertImageWithCreatedAt(UUID imageId, OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO images (id, image_url, created_at) VALUES (?, ?, ?)",
                imageId,
                imageUrl(imageId),
                createdAt);
    }

    /**
     * Builds the URL expected for a test image.
     */
    private String imageUrl(UUID imageId) {
        return "https://example.supabase.co/image/" + imageId;
    }
}
