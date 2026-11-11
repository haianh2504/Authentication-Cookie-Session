package org.example.authentication_cookiesession.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Trả lỗi 403 theo response envelope khi user đã đăng nhập nhưng không đủ quyền.
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Ghi response 403 JSON thay vì response HTML mặc định.
     *
     * @param request request bị từ chối quyền
     * @param response response REST trả về client
     * @param accessDeniedException lý do request không đủ quyền
     * @throws IOException khi không ghi được JSON response
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ApiResponse<Map<String, Object>> body = ApiResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ADMIN_ACCESS_FORBIDDEN",
                "You do not have permission to perform this action",
                Map.of());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
