package com.quill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Technology")
        String name,

        @Size(max = 1000) @Schema(example = "Posts about technology and software")
        String description) {}
