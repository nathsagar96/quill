package com.quill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PostRequest(
        @NotBlank @Size(max = 200) @Schema(example = "My First Post") String title,
        @NotBlank @Schema(example = "This is the full body of the post...") String body,
        @Size(max = 500) @Schema(example = "A short excerpt for preview") String excerpt,
        @Schema(example = "[\"1\",\"2\"]") Set<@NotNull Long> categoryIds,
        @Schema(example = "[\"3\",\"4\"]") Set<@NotNull Long> tagIds) {}
