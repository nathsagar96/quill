package com.quill.dto.response;

import com.quill.model.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

public record PostResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "My First Post") String title,

        @Schema(example = "This is the full body of the post...")
        String body,

        @Schema(example = "A short excerpt for preview") String excerpt,
        @Schema(example = "my-first-post") String slug,
        AuthorResponse author,
        @Schema(example = "[\"1\",\"2\"]") Set<Long> categoryIds,
        @Schema(example = "[\"3\",\"4\"]") Set<Long> tagIds,
        PostStatus status,
        Instant publishedAt,
        Instant scheduledAt,
        Instant createdAt,
        Instant updatedAt) {}
