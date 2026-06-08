package com.quill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(
        @NotBlank String body,
        @NotNull Long postId,
        @NotNull Long authorId) {}
