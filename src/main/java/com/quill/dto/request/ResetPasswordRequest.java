package com.quill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Schema(example = "abc123-reset-token") String token,
        @NotBlank @Size(min = 8, max = 128) @Schema(example = "NewP@ssword456", format = "password") String password) {}
