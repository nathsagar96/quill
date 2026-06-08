package com.quill.dto.response;

import java.time.Instant;
import java.util.Set;

public record PostResponse(
        Long id,
        String title,
        String body,
        String excerpt,
        String slug,
        AuthorResponse author,
        Set<Long> categoryIds,
        Set<Long> tagIds,
        Instant createdAt,
        Instant updatedAt) {}
