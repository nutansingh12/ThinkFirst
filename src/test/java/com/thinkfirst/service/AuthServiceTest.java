package com.thinkfirst.service;

import com.thinkfirst.dto.AuthResponse;
import com.thinkfirst.dto.LoginRequest;
import com.thinkfirst.dto.RegisterRequest;
import com.thinkfirst.model.User;
import com.thinkfirst.repository.UserRepository;
import com.thinkfirst.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AuthService
 * Tests user registration, login, and refresh token functionality
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("test@example.com");
        registerRequest.setPassword("Test123!");
        registerRequest.setFullName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("test@example.com");
        loginRequest.setPassword("Test123!");
    }

    // Registration Tests

    @Test
    void testRegister_WithValidData_ShouldCreateUserAndReturnTokens() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("refreshToken");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");

        // Verify
        verify(userRepository).existsByUsername("test@example.com");
        verify(passwordEncoder).encode("Test123!");
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(any(UserDetails.class));
        verify(jwtTokenProvider).generateRefreshToken(any(UserDetails.class));
    }

    @Test
    void testRegister_WithExistingUsername_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        // Verify
        verify(userRepository).existsByUsername("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    // Login Tests

    @Test
    void testLogin_WithCorrectCredentials_ShouldReturnTokens() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("refreshToken");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        // Verify
        verify(userRepository).findByUsername("test@example.com");
        verify(passwordEncoder).matches("Test123!", "encodedPassword");
        verify(jwtTokenProvider).generateToken(any(UserDetails.class));
        verify(jwtTokenProvider).generateRefreshToken(any(UserDetails.class));
    }

    @Test
    void testLogin_WithIncorrectPassword_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        // Verify
        verify(userRepository).findByUsername("test@example.com");
        verify(passwordEncoder).matches("Test123!", "encodedPassword");
        verify(jwtTokenProvider, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void testLogin_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(userRepository).findByUsername("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // Refresh Token Tests

    @Test
    void testRefreshToken_WithValidToken_ShouldReturnNewTokens() {
        // Arrange
        String refreshToken = "validRefreshToken";
        when(jwtTokenProvider.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(jwtTokenProvider.validateToken(refreshToken, testUser)).thenReturn(true);
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("newRefreshToken");

        // Act
        AuthResponse response = authService.refreshToken(refreshToken);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        assertThat(response.getUserId()).isEqualTo(1L);

        // Verify
        verify(jwtTokenProvider).extractUsername(refreshToken);
        verify(jwtTokenProvider).validateToken(refreshToken, testUser);
        verify(jwtTokenProvider).generateToken(any(UserDetails.class));
        verify(jwtTokenProvider).generateRefreshToken(any(UserDetails.class));
    }

    @Test
    void testRefreshToken_WithExpiredToken_ShouldThrowException() {
        // Arrange
        String expiredToken = "expiredRefreshToken";
        when(jwtTokenProvider.extractUsername(expiredToken)).thenReturn("test@example.com");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.validateToken(expiredToken, testUser)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(expiredToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");

        // Verify
        verify(jwtTokenProvider).extractUsername(expiredToken);
        verify(jwtTokenProvider).validateToken(expiredToken, testUser);
        verify(jwtTokenProvider, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void testRefreshToken_WithInvalidToken_ShouldThrowException() {
        // Arrange
        String invalidToken = "invalidToken";
        when(jwtTokenProvider.extractUsername(invalidToken)).thenThrow(new IllegalArgumentException("Invalid token"));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");

        // Verify
        verify(jwtTokenProvider).extractUsername(invalidToken);
        verify(jwtTokenProvider, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void testRefreshToken_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        String refreshToken = "validRefreshToken";
        when(jwtTokenProvider.extractUsername(refreshToken)).thenReturn("nonexistent@example.com");
        when(userRepository.findByUsername("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");

        // Verify
        verify(jwtTokenProvider).extractUsername(refreshToken);
        verify(userRepository).findByUsername("nonexistent@example.com");
    }

    // Token Rotation Tests

    @Test
    void testRefreshToken_ShouldGenerateNewRefreshToken() {
        // Arrange
        String oldRefreshToken = "oldRefreshToken";
        String newRefreshToken = "newRefreshToken";
        when(jwtTokenProvider.extractUsername(oldRefreshToken)).thenReturn("test@example.com");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.validateToken(oldRefreshToken, testUser)).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn(newRefreshToken);

        // Act
        AuthResponse response = authService.refreshToken(oldRefreshToken);

        // Assert - Should return new refresh token (token rotation)
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
        assertThat(response.getRefreshToken()).isNotEqualTo(oldRefreshToken);
    }
}

