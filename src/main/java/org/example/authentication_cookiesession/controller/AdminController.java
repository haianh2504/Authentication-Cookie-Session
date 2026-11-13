package org.example.authentication_cookiesession.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.dto.request.AdminLoginRequest;
import org.example.authentication_cookiesession.dto.response.ApiResponse;
import org.example.authentication_cookiesession.dto.response.AuthenticationStatusResponse;
import org.example.authentication_cookiesession.service.AdminSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điều khiển các endpoint authentication và session của administrator.
 */
@RestController
@RequestMapping("/api/v1/auth/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminSessionService adminSessionService;

    /**
     * Đăng nhập admin, tạo session Redis và trả cookie ADMIN_SESSION.
     *
     * @param loginRequest credentials đã qua validation
     * @param request HTTP request hiện tại
     * @param response HTTP response hiện tại
     * @return response 201 không chứa raw session ID
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AuthenticationStatusResponse>> login(
            @Valid @RequestBody AdminLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        adminSessionService.login(loginRequest, request, response);

        ApiResponse<AuthenticationStatusResponse> body = ApiResponse.of(
                HttpStatus.CREATED.value(),
                "ADMIN_LOGIN_SUCCESSFULLY",
                "Admin login successfully",
                new AuthenticationStatusResponse(true));

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Kiểm tra session admin hiện tại do Spring Security khôi phục.
     *
     * @param authentication authentication của request hiện tại
     * @return response 200 chỉ chứa trạng thái authenticated
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<AuthenticationStatusResponse>> getCurrentSession(
            Authentication authentication) {
        ApiResponse<AuthenticationStatusResponse> body = ApiResponse.of(
                HttpStatus.OK.value(),
                "ADMIN_SESSION_RETRIEVING_SUCCESSFULLY",
                "Admin session is valid",
                adminSessionService.getCurrentSession(authentication));

        return ResponseEntity.ok(body);
    }

    /**
     * Xóa session hiện tại trong Redis và để Spring Session gửi cookie hết hạn
     * cho browser khi response được commit.
     *
     * @param request HTTP request hiện tại
     * @return response 200 với trạng thái authenticated=false
     */
    @DeleteMapping("/sessions")
    public ResponseEntity<ApiResponse<AuthenticationStatusResponse>> logout(
            HttpServletRequest request) {
        adminSessionService.logout(request);

        ApiResponse<AuthenticationStatusResponse> body = ApiResponse.of(
                HttpStatus.OK.value(),
                "ADMIN_LOGOUT_SUCCESSFULLY",
                "Admin logout successfully",
                new AuthenticationStatusResponse(false));

        return ResponseEntity.ok(body);
    }
}
