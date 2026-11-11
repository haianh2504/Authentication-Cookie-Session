package org.example.authentication_cookiesession.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.dto.request.AdminLoginRequest;
import org.example.authentication_cookiesession.dto.response.AuthenticationStatusResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Điều phối authentication và vòng đời session của administrator.
 */
@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    /**
     * Xác thực credentials, xoay session ID và lưu SecurityContext vào session Redis.
     *
     * @param loginRequest credentials từ request login
     * @param request HTTP request hiện tại
     * @param response HTTP response hiện tại
     */
    public void login(
            AdminLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.email(),
                        loginRequest.password()));

        request.getSession(true);
        request.changeSessionId();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    /**
     * Chuyển authentication hiện tại thành dữ liệu an toàn cho UC03.
     *
     * @param authentication authentication do Spring Security khôi phục từ session
     * @return trạng thái authenticated, không chứa session ID hay password
     */
    public AuthenticationStatusResponse getCurrentSession(Authentication authentication) {
        return new AuthenticationStatusResponse(authentication != null && authentication.isAuthenticated());
    }
}
