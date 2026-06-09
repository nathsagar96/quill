package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.dto.request.LoginRequest;
import com.quill.dto.request.RefreshTokenRequest;
import com.quill.dto.request.RegisterRequest;
import com.quill.dto.response.AuthResponse;
import com.quill.dto.response.RegisterResponse;
import com.quill.exception.DuplicateEmailException;
import com.quill.exception.DuplicateUsernameException;
import com.quill.exception.PasswordResetTokenException;
import com.quill.exception.RefreshTokenException;
import com.quill.model.PasswordResetToken;
import com.quill.model.RefreshToken;
import com.quill.model.User;
import com.quill.repository.PasswordResetTokenRepository;
import com.quill.repository.RefreshTokenRepository;
import com.quill.repository.UserRepository;
import com.quill.security.CustomUserDetails;
import com.quill.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
    private static final String ACCESS_TOKEN = "jwt-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

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
                .enabled(true)
                .build();
        userDetails = new CustomUserDetails(user);
    }

    private void mockIssueRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates user as disabled, sends verification email, returns message")
        void createsUserAndSendsVerificationEmail() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RegisterResponse result = authService.register(registerRequest);

            assertThat(result.message()).contains("Registration successful");

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.isEnabled()).isFalse();
            assertThat(saved.isEmailVerified()).isFalse();
            assertThat(saved.getVerificationToken()).isNotNull();

            verify(emailService).sendVerificationEmail(eq(EMAIL), any(String.class));
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
    @DisplayName("verifyEmail")
    class VerifyEmail {

        private UUID tokenUuid;

        @BeforeEach
        void setUp() {
            tokenUuid = UUID.randomUUID();
            user.setVerificationToken(tokenUuid);
            user.setEnabled(false);
            user.setEmailVerified(false);
        }

        @Test
        @DisplayName("enables user and clears verification token")
        void verifiesEmail() {
            when(userRepository.findByVerificationToken(tokenUuid)).thenReturn(Optional.of(user));

            authService.verifyEmail(tokenUuid.toString());

            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws RefreshTokenException for unknown token")
        void throwsForUnknownToken() {
            when(userRepository.findByVerificationToken(tokenUuid)).thenReturn(Optional.empty());

            assertThrows(RefreshTokenException.class, () -> authService.verifyEmail(tokenUuid.toString()));
        }

        @Test
        @DisplayName("throws RefreshTokenException for invalid UUID format")
        void throwsForInvalidUuid() {
            assertThrows(RefreshTokenException.class, () -> authService.verifyEmail("not-a-uuid"));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("authenticates and returns auth response with both tokens")
        void authenticatesAndReturnsAuthResponse() {
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.generateToken(userDetails)).thenReturn(ACCESS_TOKEN);
            mockIssueRefreshToken();

            AuthResponse result = authService.login(loginRequest);

            assertThat(result.token()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isNotNull().isNotEmpty();
            assertThat(result.username()).isEqualTo(USERNAME);

            verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
            assertThat(refreshTokenCaptor.getValue().getUser()).isEqualTo(user);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        private UUID tokenUuid;
        private RefreshToken storedToken;

        @BeforeEach
        void setUp() {
            tokenUuid = UUID.randomUUID();
            storedToken = RefreshToken.builder()
                    .id(1L)
                    .token(tokenUuid)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();
        }

        @Test
        @DisplayName("returns new auth response with rotated tokens")
        void returnsRotatedTokens() {
            when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(storedToken));
            when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn(ACCESS_TOKEN);
            mockIssueRefreshToken();

            AuthResponse result = authService.refresh(new RefreshTokenRequest(tokenUuid.toString()));

            assertThat(result.token()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isNotNull().isNotEmpty();
            assertThat(result.username()).isEqualTo(USERNAME);
            assertThat(storedToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("throws RefreshTokenException for revoked token")
        void throwsForRevokedToken() {
            storedToken.setRevoked(true);
            when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(storedToken));

            assertThrows(
                    RefreshTokenException.class,
                    () -> authService.refresh(new RefreshTokenRequest(tokenUuid.toString())));
        }

        @Test
        @DisplayName("throws RefreshTokenException for expired token")
        void throwsForExpiredToken() {
            storedToken.setExpiresAt(Instant.now().minusSeconds(1));
            when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(storedToken));

            assertThrows(
                    RefreshTokenException.class,
                    () -> authService.refresh(new RefreshTokenRequest(tokenUuid.toString())));
        }

        @Test
        @DisplayName("throws RefreshTokenException for unknown token")
        void throwsForUnknownToken() {
            when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.empty());

            assertThrows(
                    RefreshTokenException.class,
                    () -> authService.refresh(new RefreshTokenRequest(tokenUuid.toString())));
        }

        @Test
        @DisplayName("throws RefreshTokenException for invalid UUID format")
        void throwsForInvalidUuid() {
            assertThrows(RefreshTokenException.class, () -> authService.refresh(new RefreshTokenRequest("not-a-uuid")));
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("revokes the refresh token")
        void revokesToken() {
            UUID tokenUuid = UUID.randomUUID();
            RefreshToken stored = RefreshToken.builder()
                    .id(1L)
                    .token(tokenUuid)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();
            when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(stored));

            authService.logout(new RefreshTokenRequest(tokenUuid.toString()));

            assertThat(stored.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(stored);
        }

        @Test
        @DisplayName("does nothing for unknown token")
        void doesNothingForUnknownToken() {
            UUID tokenUuid = UUID.randomUUID();
            when(refreshTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.empty());

            authService.logout(new RefreshTokenRequest(tokenUuid.toString()));
        }

        @Test
        @DisplayName("does nothing for invalid UUID format")
        void doesNothingForInvalidUuid() {
            authService.logout(new RefreshTokenRequest("not-a-uuid"));
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("creates reset token and sends email when user exists")
        void sendsResetEmailForExistingUser() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            authService.forgotPassword(EMAIL);

            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq(EMAIL), any(String.class));
        }

        @Test
        @DisplayName("does nothing when email does not exist")
        void doesNothingForUnknownEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            authService.forgotPassword(EMAIL);

            verify(passwordResetTokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(any(), any());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private UUID tokenUuid;
        private PasswordResetToken storedToken;
        private static final String NEW_PASSWORD = "new-password-123";

        @BeforeEach
        void setUp() {
            tokenUuid = UUID.randomUUID();
            storedToken = PasswordResetToken.builder()
                    .id(1L)
                    .token(tokenUuid)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .used(false)
                    .build();
        }

        @Test
        @DisplayName("resets password with valid token")
        void resetsPassword() {
            when(passwordResetTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(storedToken));
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("new-encoded");

            authService.resetPassword(tokenUuid.toString(), NEW_PASSWORD);

            verify(passwordResetTokenRepository).save(storedToken);
            assertThat(storedToken.isUsed()).isTrue();
            verify(userRepository).save(user);
            assertThat(user.getPasswordHash()).isEqualTo("new-encoded");
        }

        @Test
        @DisplayName("throws PasswordResetTokenException for used token")
        void throwsForUsedToken() {
            storedToken.setUsed(true);
            when(passwordResetTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(storedToken));

            assertThrows(
                    PasswordResetTokenException.class,
                    () -> authService.resetPassword(tokenUuid.toString(), NEW_PASSWORD));
        }

        @Test
        @DisplayName("throws PasswordResetTokenException for expired token")
        void throwsForExpiredToken() {
            storedToken.setExpiresAt(Instant.now().minusSeconds(1));
            when(passwordResetTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(storedToken));

            assertThrows(
                    PasswordResetTokenException.class,
                    () -> authService.resetPassword(tokenUuid.toString(), NEW_PASSWORD));
        }

        @Test
        @DisplayName("throws PasswordResetTokenException for unknown token")
        void throwsForUnknownToken() {
            when(passwordResetTokenRepository.findByToken(tokenUuid)).thenReturn(Optional.empty());

            assertThrows(
                    PasswordResetTokenException.class,
                    () -> authService.resetPassword(tokenUuid.toString(), NEW_PASSWORD));
        }

        @Test
        @DisplayName("throws PasswordResetTokenException for invalid UUID format")
        void throwsForInvalidUuid() {
            assertThrows(
                    PasswordResetTokenException.class, () -> authService.resetPassword("not-a-uuid", NEW_PASSWORD));
        }
    }
}
