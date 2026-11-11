package org.example.authentication_cookiesession.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.authentication_cookiesession.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Cung cấp CSRF token cho frontend trước các request thay đổi trạng thái.
 */
@RestController
@RequestMapping("/api/v1/csrf-token")
public class CsrfController {

    /**
     * Kích hoạt việc phát token vào cookie XSRF-TOKEN và trả envelope rỗng.
     *
     * @param csrfToken token do Spring Security quản lý
     * @return response 200 với data là object rỗng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();

        return ResponseEntity.ok(ApiResponse.of(
                HttpServletResponse.SC_OK,
                "CSRF_TOKEN_RETRIEVING_SUCCESSFULLY",
                "CSRF token retrieved successfully",
                Map.of()));
    }
}
