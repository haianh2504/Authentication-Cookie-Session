package org.example.authentication_cookiesession.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {

    @Id
    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Tạo entity admin mới từ email và password hash đã được mã hóa.
     *
     * @param email email định danh duy nhất của admin
     * @param passwordHash mật khẩu đã được hash, không phải plaintext
     * @return entity admin sẵn sàng để persistence layer lưu xuống database
     */
    public static Admin create(String email, String passwordHash) {
        Admin admin = new Admin();
        admin.email = email;
        admin.passwordHash = passwordHash;
        return admin;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
