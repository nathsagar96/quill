package com.quill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        @NotEmpty Set<@NotNull Long> categoryIds,
        Set<@NotNull Long> tagIds) {}
