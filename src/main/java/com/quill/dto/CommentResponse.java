package com.quill.dto;

import java.time.Instant;

public record CommentResponse(Long id, String body, Long postId, Long authorId, Instant createdAt, Instant updatedAt) {}
