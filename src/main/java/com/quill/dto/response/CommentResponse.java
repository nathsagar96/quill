package com.quill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CommentResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Great post! Thanks for sharing.") String body,
        @Schema(example = "1") Long postId,
        AuthorResponse author,
        Instant createdAt,
        Instant updatedAt) {}
