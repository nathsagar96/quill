package com.quill.dto;

public record AuthResponse(
        String token, String tokenType, Long userId, String username, String email, String displayName) {}
