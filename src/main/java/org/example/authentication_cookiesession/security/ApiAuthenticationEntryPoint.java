package org.example.authentication_cookiesession.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Trả lỗi 401 theo response envelope khi filter chain chặn request chưa xác thực.
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Ghi response 401 mà không chuyển hướng sang form login HTML.
     *
     * @param request request bị từ chối
     * @param response response REST trả về client
     * @param authException lý do authentication thất bại
     * @throws IOException khi không ghi được JSON response
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "ADMIN_SESSION_UNAUTHORIZED",
                "Your session is invalid or has expired. Please sign in again.",
                Map.of());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
