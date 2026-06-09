package com.quill.controller;

import com.quill.dto.request.TagRequest;
import com.quill.dto.response.TagResponse;
import com.quill.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tags", description = "Blog post tags")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "List all tags", description = "Returns all tags. Does not require authentication.")
    @ApiResponse(responseCode = "200", description = "List of tags", useReturnTypeSchema = true)
    public ResponseEntity<List<TagResponse>> findAllTags() {
        return ResponseEntity.ok(tagService.findAllTags());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID", description = "Returns a single tag by its unique identifier.")
    @ApiResponse(responseCode = "200", description = "Tag found", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "404",
            description = "Tag not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<TagResponse> findTagById(
            @Parameter(description = "Tag ID", example = "1", required = true)
            @Min(1) @PathVariable Long id) {
        return ResponseEntity.ok(tagService.findTagById(id));
    }

    @PostMapping
    @Operation(summary = "Create a tag", description = "Creates a new tag. Requires authentication.")
    @ApiResponse(responseCode = "201", description = "Tag created", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.createTag(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a tag", description = "Updates an existing tag. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Tag updated", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Tag not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<TagResponse> updateTag(
            @Parameter(description = "Tag ID", example = "1", required = true)
            @Min(1) @PathVariable Long id,
            @Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(tagService.updateTag(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a tag",
            description = "Deletes a tag by ID. Requires admin role.")
    @ApiResponse(responseCode = "204", description = "Tag deleted")
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden — admin role required",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Tag not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteTag(
            @Parameter(description = "Tag ID", example = "1", required = true)
            @Min(1) @PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
