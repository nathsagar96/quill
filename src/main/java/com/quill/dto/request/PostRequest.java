package com.quill.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        @Size(max = 500) String excerpt,
        Set<@NotNull Long> categoryIds,
        Set<@NotNull Long> tagIds) {}
