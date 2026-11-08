package org.example.authentication_cookiesession;

import org.springframework.boot.SpringApplication;

public class TestAuthenticationCookieSessionApplication {

    public static void main(String[] args) {
        SpringApplication.from(AuthenticationCookieSessionApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
