package org.example.authentication_cookiesession;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Annotation tổng hợp của Spring Boot, bao gồm:
 @Configuration
 @EnableAutoConfiguration
 @ComponentScan
 Nó giúp Spring Boot tìm các @Component, @Service, @Repository, @Configuration.
 **/
@SpringBootApplication


/** tự động tìm các class có:
 @ConfigurationProperties
 **/
@ConfigurationPropertiesScan
public class AuthenticationCookieSessionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticationCookieSessionApplication.class, args);
    }

}
