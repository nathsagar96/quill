package com.quill.dto.response;

public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType,
        Long userId,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl) {}
