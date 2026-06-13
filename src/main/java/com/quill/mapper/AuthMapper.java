package com.quill.mapper;

import com.quill.dto.response.AuthResponse;
import com.quill.model.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthResponse toAuthResponse(String token, String refreshToken, User user) {
        return new AuthResponse(
                token,
                refreshToken,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl());
    }
}
