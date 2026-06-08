package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.dto.request.LoginRequest;
import com.quill.dto.request.RegisterRequest;
import com.quill.dto.response.AuthResponse;
import com.quill.exception.DuplicateEmailException;
import com.quill.exception.DuplicateUsernameException;
import com.quill.model.User;
import com.quill.repository.UserRepository;
import com.quill.security.CustomUserDetails;
import com.quill.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private static final String USERNAME = "alice";
    private static final String EMAIL = "alice@example.com";
    private static final String PASSWORD = "password123";
    private static final String TOKEN = "jwt-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        registerRequest =
                new RegisterRequest(USERNAME, EMAIL, PASSWORD, "Alice", "A bio", "https://example.com/avatar.jpg");
        loginRequest = new LoginRequest(USERNAME, PASSWORD);
        user = User.builder()
                .id(1L)
                .username(USERNAME)
                .email(EMAIL)
                .passwordHash("encoded-password")
                .displayName("Alice")
                .bio("A bio")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();
        userDetails = new CustomUserDetails(user);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates user and returns auth response")
        void createsUserAndReturnsAuthResponse() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);

            AuthResponse result = authService.register(registerRequest);

            assertThat(result.token()).isEqualTo(TOKEN);
            assertThat(result.tokenType()).isEqualTo("Bearer");
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.username()).isEqualTo(USERNAME);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.displayName()).isEqualTo("Alice");
            assertThat(result.bio()).isEqualTo("A bio");
            assertThat(result.avatarUrl()).isEqualTo("https://example.com/avatar.jpg");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws DuplicateUsernameException when username already exists")
        void throwsWhenUsernameTaken() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThrows(DuplicateUsernameException.class, () -> authService.register(registerRequest));
        }

        @Test
        @DisplayName("throws DuplicateEmailException when email already exists")
        void throwsWhenEmailTaken() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThrows(DuplicateEmailException.class, () -> authService.register(registerRequest));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("authenticates and returns auth response")
        void authenticatesAndReturnsAuthResponse() {
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.generateToken(userDetails)).thenReturn(TOKEN);

            AuthResponse result = authService.login(loginRequest);

            assertThat(result.token()).isEqualTo(TOKEN);
            assertThat(result.username()).isEqualTo(USERNAME);
        }
    }
}
