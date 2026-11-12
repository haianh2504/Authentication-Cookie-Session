package org.example.authentication_cookiesession;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Cung cấp PostgreSQL container dùng chung cho các JPA slice integration test.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfiguration {

    /**
     * Khởi động PostgreSQL và để Spring Boot tự tạo datasource từ connection details.
     *
     * @return PostgreSQL container dùng cho test
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    }
}
