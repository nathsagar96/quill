package com.quill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String token,
        @Schema(example = "dGhpcyBpcyBhIHJlZnJl...") String refreshToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "1") Long userId,
        @Schema(example = "johndoe") String username,
        @Schema(example = "john@example.com") String email,
        @Schema(example = "John Doe") String displayName,
        @Schema(example = "Writer and blogger") String bio,
        @Schema(example = "https://example.com/avatar.jpg") String avatarUrl) {}
