package com.quill.dto.response;

import java.time.Instant;

public record CommentResponse(
        Long id, String body, Long postId, AuthorResponse author, Instant createdAt, Instant updatedAt) {}
