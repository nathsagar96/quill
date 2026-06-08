package com.quill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,

        @NotBlank
        @Size(max = 120)
        @Pattern(regexp = "[a-z0-9-]+", message = "slug must contain only lowercase letters, digits, and hyphens")
        String slug,

        @Size(max = 1000) String description) {}
