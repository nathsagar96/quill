package com.quill.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Size(max = 500) String bio,
        @Size(max = 255) String avatarUrl) {}
