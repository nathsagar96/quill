package com.quill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) @Schema(example = "John Doe") String displayName,
        @Size(max = 500) @Schema(example = "Writer and blogger") String bio,
        @Size(max = 255) @Schema(example = "https://example.com/avatar.jpg") String avatarUrl) {}
