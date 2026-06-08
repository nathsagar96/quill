package com.quill.dto;

import java.time.Instant;

public record PostResponse(Long id, String title, String body, Long authorId, Instant createdAt, Instant updatedAt) {}
