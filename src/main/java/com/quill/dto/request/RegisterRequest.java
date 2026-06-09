package com.quill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Schema(example = "johndoe")
        String username,

        @NotBlank @Email @Size(max = 255) @Schema(example = "john@example.com")
        String email,

        @NotBlank @Size(min = 8, max = 128) @Schema(example = "P@ssword123", format = "password")
        String password,

        @Size(max = 100) @Schema(example = "John Doe") String displayName,

        @Size(max = 500) @Schema(example = "Writer and blogger")
        String bio,

        @Size(max = 255) @Schema(example = "https://example.com/avatar.jpg")
        String avatarUrl) {}
