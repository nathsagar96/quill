package com.quill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank @Schema(example = "Great post! Thanks for sharing.")
        String body) {}
