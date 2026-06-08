package com.quill.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(@NotBlank String body) {}
