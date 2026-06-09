package com.quill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Schema(example = "johndoe") String username,
        @NotBlank @Schema(example = "P@ssword123", format = "password") String password) {}
