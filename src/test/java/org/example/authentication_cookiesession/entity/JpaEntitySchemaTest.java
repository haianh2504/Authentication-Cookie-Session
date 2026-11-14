package org.example.authentication_cookiesession.entity;

import org.example.authentication_cookiesession.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Not using in-memory
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

// import cấu hình test tạo PostgreSQL container
@Import(PostgresTestcontainersConfiguration.class)

// Tạo JPA test slice, gồm: Hibernate / Entity Manager / JPA repositories / Transaction rollback
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration"
})
class JpaEntitySchemaTest {

    @Autowired
    // EntityManager là API chuẩn của JPA để làm việc với entity
    private EntityManager entityManager;

    @Test
    void persistsAndReadsImageEntityAgainstPostgreSqlSchema() {
        Image image = new Image();
        image.setImageUrl("https://example.supabase.co/image/test-image");
        image.setCreatedAt(OffsetDateTime.now());
        // Hibernate sẽ tự tạo UUID.

        entityManager.persist(image); // Đưa object image vào persistence context của JPA.
        // Hibernate bắt đầu quản lý entity này và chuẩn bị câu SQL

        // Ép Hibernate gửi SQL thật xuống PostgreSQL ngay lúc này
        entityManager.flush();
        // Vi phạm constraint, lỗi sẽ xuất hiện tại đây.

        // Xóa entity khỏi first-level cache của JPA.
        entityManager.clear();

        // Hibernate chạy SQL SELECT thật để đọc lại dữ liệu từ PostgreSQL.
        Image persistedImage = entityManager.find(Image.class, image.getId());

        assertNotNull(persistedImage);
        assertNotNull(persistedImage.getId());
        assertEquals(image.getImageUrl(), persistedImage.getImageUrl());
        assertEquals(
                image.getCreatedAt().toInstant(),
                persistedImage.getCreatedAt().toInstant());
    }

    @Test
    void persistsAndReadsAdminEntityAgainstPostgreSqlSchema() {
        Admin admin = Admin.create("admin@example.com", "bcrypt-hash");

        entityManager.persist(admin);
        entityManager.flush();
        entityManager.clear();

        Admin persistedAdmin = entityManager.find(Admin.class, admin.getEmail());

        assertNotNull(persistedAdmin);
        assertEquals(admin.getEmail(), persistedAdmin.getEmail());
        assertEquals(admin.getPasswordHash(), persistedAdmin.getPasswordHash());
    }

}
