package com.smashmate.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateAndValidate_accessToken() {
        String token = jwtService.generateAccessToken("admin@smashmate.local", "ADMIN");
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@smashmate.local");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }
}
