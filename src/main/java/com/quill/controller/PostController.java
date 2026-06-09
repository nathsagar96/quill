package com.quill.controller;

import com.quill.dto.request.PostRequest;
import com.quill.dto.response.PostResponse;
import com.quill.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Posts", description = "Blog post CRUD operations")
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(
            summary = "List posts",
            description = "Returns a paginated list of posts sorted by creation date descending. "
                    + "Optionally filter by category ID or tag ID.")
    @ApiResponse(
            responseCode = "200",
            description = "Paginated list of posts",
            useReturnTypeSchema = true)
    public ResponseEntity<Page<PostResponse>> findAllPosts(
            @Parameter(description = "Filter by category ID", example = "1")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by tag ID", example = "1")
            @RequestParam(required = false) Long tagId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true)
            Pageable pageable) {
        if (categoryId != null) {
            return ResponseEntity.ok(postService.findPostsByCategoryId(categoryId, pageable));
        }
        if (tagId != null) {
            return ResponseEntity.ok(postService.findPostsByTagId(tagId, pageable));
        }
        return ResponseEntity.ok(postService.findAllPosts(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID", description = "Returns a single post by its unique identifier.")
    @ApiResponse(responseCode = "200", description = "Post found", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PostResponse> findPostById(
            @Parameter(description = "Post ID", example = "1", required = true)
            @Min(1) @PathVariable Long id) {
        return ResponseEntity.ok(postService.findPostById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get post by slug", description = "Returns a single post by its URL-friendly slug.")
    @ApiResponse(responseCode = "200", description = "Post found", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PostResponse> findPostBySlug(
            @Parameter(description = "Post slug", example = "my-first-post", required = true)
            @PathVariable String slug) {
        return ResponseEntity.ok(postService.findPostBySlug(slug));
    }

    @PostMapping
    @Operation(
            summary = "Create a post",
            description = "Creates a new blog post. Requires authentication. "
                    + "The authenticated user is set as the author.")
    @ApiResponse(responseCode = "201", description = "Post created", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request, Authentication authentication) {
        PostResponse response = postService.createPost(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a post",
            description = "Updates an existing post. Only the author can update. "
                    + "Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Post updated", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden — not the author",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "Post ID", example = "1", required = true)
            @Min(1) @PathVariable Long id,
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {
        PostResponse response = postService.updatePost(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a post",
            description = "Deletes a post by ID. Requires admin role.")
    @ApiResponse(responseCode = "204", description = "Post deleted")
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
            description = "Post not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "Post ID", example = "1", required = true)
            @Min(1) @PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
