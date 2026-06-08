package com.quill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(@NotBlank @Size(max = 50) String name) {}
