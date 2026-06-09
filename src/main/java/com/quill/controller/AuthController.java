package com.quill.controller;

import com.quill.dto.request.ForgotPasswordRequest;
import com.quill.dto.request.LoginRequest;
import com.quill.dto.request.RefreshTokenRequest;
import com.quill.dto.request.RegisterRequest;
import com.quill.dto.request.ResetPasswordRequest;
import com.quill.dto.request.VerifyEmailRequest;
import com.quill.dto.response.AuthResponse;
import com.quill.dto.response.RegisterResponse;
import com.quill.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "User registration, login, token refresh, password reset, and email verification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account. A verification email may be sent. "
                    + "Does not require authentication.",
            security = {})
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(
            responseCode = "409",
            description = "Username or email already exists",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verify email address",
            description = "Confirms a user's email address using the token sent during registration. "
                    + "Does not require authentication.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Email verified")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid or expired token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset email to the given address if it exists. "
                    + "Does not reveal whether the account exists. Does not require authentication.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Password reset email sent")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password with token",
            description =
                    "Resets the password using the token received via email. " + "Does not require authentication.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid or expired reset token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate and get tokens",
            description = "Authenticates with username and password. Returns access and refresh tokens. "
                    + "Does not require authentication.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Login successful", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new access token and refresh token pair. "
                    + "Does not require authentication.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Tokens refreshed", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout and invalidate refresh token",
            description = "Invalidates the provided refresh token. Requires authentication.")
    @ApiResponse(responseCode = "204", description = "Logged out successfully")
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
