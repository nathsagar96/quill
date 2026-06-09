package com.quill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterResponse(
        @Schema(example = "Registration successful. Please check your email to verify your account.")
        String message) {}
