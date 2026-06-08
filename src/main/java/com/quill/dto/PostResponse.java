package com.quill.dto;

import java.time.Instant;
import java.util.Set;

public record PostResponse(
        Long id,
        String title,
        String body,
        String excerpt,
        Long authorId,
        Set<Long> categoryIds,
        Set<Long> tagIds,
        Instant createdAt,
        Instant updatedAt) {}
