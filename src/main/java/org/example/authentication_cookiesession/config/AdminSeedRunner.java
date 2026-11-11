package org.example.authentication_cookiesession.config;

import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.service.AdminSeedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kích hoạt seed admin sau khi Spring Boot
 * đã khởi tạo đầy đủ repository và datasource.
 */
@Component
@RequiredArgsConstructor
// chỉ tạo ben khi property có giá trị: app.admin.seed.enabled=true
@ConditionalOnProperty(prefix = "app.admin.seed", name = "enabled", havingValue = "true")
public class AdminSeedRunner implements ApplicationRunner {

    private final AdminSeedService adminSeedService;

    /**
     * Chạy seed có kiểm soát khi ứng dụng khởi động.
     * @param arguments tham số dòng lệnh của ứng dụng
     */
    @Override
    public void run(ApplicationArguments arguments) {
        adminSeedService.seedIfNecessary();
    }
}
