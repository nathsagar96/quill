package com.quill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CategoryResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Technology") String name,
        @Schema(example = "technology") String slug,
        @Schema(example = "Posts about technology and software") String description,
        Instant createdAt,
        Instant updatedAt) {}
