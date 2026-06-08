package com.quill.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(@NotBlank String body) {}
