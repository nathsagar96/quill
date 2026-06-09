package com.quill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthorResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "johndoe") String username,
        @Schema(example = "John Doe") String displayName,
        @Schema(example = "Writer and blogger") String bio,
        @Schema(example = "https://example.com/avatar.jpg") String avatarUrl) {}
