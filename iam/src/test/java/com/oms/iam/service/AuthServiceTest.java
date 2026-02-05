package com.oms.iam.service;

import com.oms.iam.dto.AuthDto;
import com.oms.iam.model.Role;
import com.oms.iam.model.Status;
import com.oms.iam.model.User;
import com.oms.iam.repository.UserRepository;
import com.oms.iam.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private AuthDto.AuthRequest validRegisterRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new AuthDto.AuthRequest();
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");
        validRegisterRequest.setName("Test User");
        validRegisterRequest.setRole(Role.USER);

        mockUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .name("Test User")
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.findByEmail(validRegisterRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act & Assert
        assertDoesNotThrow(() -> authService.register(validRegisterRequest));

        verify(userRepository).findByEmail(validRegisterRequest.getEmail());
        verify(passwordEncoder).encode(validRegisterRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateEmail() {
        // Arrange
        when(userRepository.findByEmail(validRegisterRequest.getEmail())).thenReturn(Optional.of(mockUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(validRegisterRequest));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).findByEmail(validRegisterRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        AuthDto.AuthRequest loginRequest = new AuthDto.AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(mockUser)).thenReturn("jwt-token-123");

        // Act
        AuthDto.AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertEquals(mockUser.getId(), response.getUserId());
        assertEquals(mockUser.getEmail(), response.getEmail());
        assertEquals(mockUser.getRole(), response.getRole());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), mockUser.getPasswordHash());
        verify(jwtUtil).generateToken(mockUser);
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        AuthDto.AuthRequest loginRequest = new AuthDto.AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongpassword");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), mockUser.getPasswordHash());
        verify(jwtUtil, never()).generateToken(any(User.class));
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        AuthDto.AuthRequest loginRequest = new AuthDto.AuthRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLogin_InactiveUser() {
        // Arrange
        AuthDto.AuthRequest loginRequest = new AuthDto.AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        User inactiveUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .name("Test User")
                .role(Role.USER)
                .status(Status.BLOCKED)
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), inactiveUser.getPasswordHash())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));

        assertEquals("User is not active", exception.getMessage());
        verify(jwtUtil, never()).generateToken(any(User.class));
    }
}
