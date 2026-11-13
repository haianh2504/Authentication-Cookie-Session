package org.example.authentication_cookiesession.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Cấu hình các bộ lọc bảo mật, phân quyền và cơ chế session của REST API.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String SESSION_COOKIE_NAME = "ADMIN_SESSION";
    private static final String CSRF_ENDPOINT = "/api/v1/csrf-token";

    /**
     * Tạo cookie serializer cho Spring Session với tên cookie opaque dành riêng cho admin.
     *
     * @param secureCookie bật cờ Secure khi ứng dụng chạy qua HTTPS
     * @return cấu hình cookie session dùng bởi Spring Session
     */
    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${app.session.cookie.secure:false}") boolean secureCookie) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(SESSION_COOKIE_NAME);
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secureCookie);
        serializer.setSameSite("Lax");
        return serializer;
    }

    /**
     * Khai báo toàn bộ luật bảo vệ endpoint và kích hoạt session fixation protection.
     *
     * @param http đối tượng xây dựng Spring Security filter chain
     * @param securityContextRepository nơi lưu và khôi phục SecurityContext từ session
     * @param authenticationEntryPoint handler lỗi 401 dạng JSON
     * @param accessDeniedHandler handler lỗi 403 dạng JSON
     * @return filter chain được Spring Security sử dụng cho mỗi request
     * @throws Exception khi Spring Security không thể xây dựng filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(CSRF_ENDPOINT))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, CSRF_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/admin").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/images").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/admin/sessions")
                        .hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/admin/sessions")
                        .hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/images").hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/images/**").hasRole(ADMIN_ROLE)
                        .anyRequest().authenticated())
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sessionFixation -> sessionFixation.changeSessionId()))
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    /**
     * Cung cấp AuthenticationManager để login service sử dụng ở bước triển khai tiếp theo.
     *
     * @param configuration cấu hình authentication do Spring Boot tạo từ UserDetailsService và PasswordEncoder
     * @return authentication manager của ứng dụng
     * @throws Exception khi Spring Security không thể tạo authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Lưu SecurityContext trong HttpSession để Spring Session đồng bộ dữ liệu vào Redis.
     *
     * @return repository quản lý SecurityContext theo session hiện tại
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
