package com.quill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record TagResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "java") String name,
        @Schema(example = "java") String slug,
        Instant createdAt,
        Instant updatedAt) {}
