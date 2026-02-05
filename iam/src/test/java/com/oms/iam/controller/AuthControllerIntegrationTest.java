package com.oms.iam.controller;

import com.oms.iam.dto.AuthDto;
import com.oms.iam.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRegisterAndLogin_Success() {
        // 1. Register
        AuthDto.AuthRequest regRequest = AuthDto.AuthRequest.builder()
                .email("test@example.com")
                .password("password")
                .name("Test User")
                .role(Role.USER)
                .build();

        ResponseEntity<String> regResponse = restTemplate.postForEntity("/auth/register", regRequest, String.class);
        assertEquals(HttpStatus.OK, regResponse.getStatusCode());
        assertEquals("User registered successfully", regResponse.getBody());

        // 2. Login
        AuthDto.AuthRequest loginRequest = AuthDto.AuthRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        ResponseEntity<AuthDto.AuthResponse> loginResponse = restTemplate.postForEntity("/auth/login", loginRequest,
                AuthDto.AuthResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getToken());
        assertEquals("test@example.com", loginResponse.getBody().getEmail());
    }

    @Test
    void testRegister_DuplicateEmail_ReturnsConflict() {
        AuthDto.AuthRequest request = AuthDto.AuthRequest.builder()
                .email("duplicate@example.com")
                .password("password")
                .name("User")
                .build();

        restTemplate.postForEntity("/auth/register", request, String.class);
        ResponseEntity<Object> response = restTemplate.postForEntity("/auth/register", request, Object.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testLogin_InvalidCredentials_ReturnsUnauthorized() {
        AuthDto.AuthRequest request = AuthDto.AuthRequest.builder()
                .email("wrong@example.com")
                .password("wrong")
                .build();

        ResponseEntity<Object> response = restTemplate.postForEntity("/auth/login", request, Object.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
