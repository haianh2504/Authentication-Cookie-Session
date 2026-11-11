package org.example.authentication_cookiesession.security;

import lombok.RequiredArgsConstructor;
import org.example.authentication_cookiesession.entity.Admin;
import org.example.authentication_cookiesession.repository.AdminRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Nạp thông tin administrator từ database để Spring Security xác thực.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    /**
     * Tìm administrator theo email và cấp quyền ROLE_ADMIN cho phiên xác thực.
     *
     * @param username email được Spring Security nhận từ thông tin đăng nhập
     * @return thông tin đăng nhập tương thích với Spring Security
     * @throws UsernameNotFoundException khi email không tồn tại
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin account was not found"));

        return User.withUsername(admin.getEmail())
                .password(admin.getPasswordHash())
                .roles("ADMIN")
                .build();
    }
}
