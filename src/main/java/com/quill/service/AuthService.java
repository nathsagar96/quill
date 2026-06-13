package com.quill.service;

import com.quill.dto.request.LoginRequest;
import com.quill.dto.request.RefreshTokenRequest;
import com.quill.dto.request.RegisterRequest;
import com.quill.dto.response.AuthResponse;
import com.quill.dto.response.RegisterResponse;
import com.quill.event.PasswordResetRequestedEvent;
import com.quill.event.UserRegisteredEvent;
import com.quill.exception.DuplicateEmailException;
import com.quill.exception.DuplicateUsernameException;
import com.quill.exception.EmailVerificationException;
import com.quill.exception.PasswordResetTokenException;
import com.quill.exception.RefreshTokenException;
import com.quill.mapper.AuthMapper;
import com.quill.model.PasswordResetToken;
import com.quill.model.RefreshToken;
import com.quill.model.User;
import com.quill.repository.PasswordResetTokenRepository;
import com.quill.repository.RefreshTokenRepository;
import com.quill.repository.UserRepository;
import com.quill.security.CustomUserDetails;
import com.quill.security.JwtService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);
    private static final Duration PASSWORD_RESET_EXPIRY = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthMapper authMapper;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registering user: username='{}', email='{}'", request.username(), request.email());

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DuplicateUsernameException(request.username());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .bio(request.bio())
                .avatarUrl(request.avatarUrl())
                .enabled(false)
                .emailVerified(false)
                .verificationToken(UUID.fromString(verificationToken))
                .build();

        User saved = userRepository.save(user);
        log.info("Registered user with id={}", saved.getId());

        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getEmail(), verificationToken));

        return new RegisterResponse("Registration successful. Please check your email to verify your account.");
    }

    @Transactional
    public void verifyEmail(String token) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new EmailVerificationException("Invalid verification token");
        }

        User user = userRepository
                .findByVerificationToken(tokenUuid)
                .orElseThrow(() -> new EmailVerificationException("Invalid verification token"));

        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        log.info("Verified email for user id={}", user.getId());
    }

    @Transactional
    public void forgotPassword(String email) {
        log.info("Password reset requested for email='{}'", email);

        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(UUID.randomUUID())
                    .user(user)
                    .expiresAt(Instant.now().plus(PASSWORD_RESET_EXPIRY))
                    .build();
            passwordResetTokenRepository.save(token);
            eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                    user.getEmail(), token.getToken().toString()));
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new PasswordResetTokenException("Invalid password reset token");
        }

        PasswordResetToken stored = passwordResetTokenRepository
                .findByToken(tokenUuid)
                .orElseThrow(() -> new PasswordResetTokenException("Password reset token not found"));

        if (stored.isUsed()) {
            throw new PasswordResetTokenException("Password reset token has already been used");
        }

        if (Instant.now().isAfter(stored.getExpiresAt())) {
            throw new PasswordResetTokenException("Password reset token has expired");
        }

        stored.setUsed(true);

        User user = stored.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        log.info("Password reset for user id={}", user.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: username='{}'", request.username());

        Authentication authenticated = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        var principal = (CustomUserDetails) authenticated.getPrincipal();
        User user = principal.user();

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = issueRefreshToken(user);
        return authMapper.toAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(request.refreshToken());
        } catch (IllegalArgumentException e) {
            throw new RefreshTokenException("Invalid refresh token format");
        }

        RefreshToken stored = refreshTokenRepository
                .findByToken(tokenUuid)
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            throw new RefreshTokenException("Refresh token has been revoked");
        }

        if (Instant.now().isAfter(stored.getExpiresAt())) {
            throw new RefreshTokenException("Refresh token has expired");
        }

        stored.setRevoked(true);

        User user = stored.getUser();
        UserDetails userDetails = new CustomUserDetails(user);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = issueRefreshToken(user);
        return authMapper.toAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        try {
            UUID tokenUuid = UUID.fromString(request.refreshToken());
            refreshTokenRepository.findByToken(tokenUuid).ifPresent(token -> {
                token.setRevoked(true);
            });
        } catch (IllegalArgumentException e) {
            // Invalid UUID format — nothing to revoke
        }
    }

    private String issueRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID())
                .user(user)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_EXPIRY))
                .build();
        refreshTokenRepository.save(token);
        return token.getToken().toString();
    }
}
