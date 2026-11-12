package org.example.authentication_cookiesession.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Khai báo HTTP client dùng riêng cho các request tới Supabase Storage.
 */
@Configuration
public class SupabaseStorageConfiguration {

    /**
     * Tạo bean RestClient để SupabaseImageStorage có thể được Spring inject rõ ràng.
     *
     * @return HTTP client dùng cho Storage REST API
     */
    @Bean
    public RestClient supabaseRestClient() {
        return RestClient.builder().build();
    }
}
