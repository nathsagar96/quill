package com.quill.controller;

import com.quill.dto.request.CommentRequest;
import com.quill.dto.response.CommentResponse;
import com.quill.service.CommentService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
@Validated
@Tag(name = "Comments", description = "Comments on blog posts")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(
            summary = "List comments for a post",
            description = "Returns a paginated list of comments for a given post, "
                    + "sorted by creation date descending. Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Paginated list of comments", useReturnTypeSchema = true)
    @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Page<CommentResponse>> findAllCommentsByPostId(
            @Parameter(description = "Post ID", example = "1", required = true)
            @Min(1) @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true)
            Pageable pageable) {
        return ResponseEntity.ok(commentService.findAllCommentsByPostId(postId, pageable));
    }

    @PostMapping
    @Operation(
            summary = "Create a comment on a post",
            description = "Adds a comment to a post. Requires authentication.")
    @ApiResponse(responseCode = "201", description = "Comment created", useReturnTypeSchema = true)
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
            description = "Post not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "Post ID", example = "1", required = true)
            @Min(1) @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        CommentResponse response = commentService.createComment(request, postId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a comment",
            description = "Updates an existing comment. Only the author can update. "
                    + "Requires authentication.")
    @ApiResponse(responseCode = "200", description = "Comment updated", useReturnTypeSchema = true)
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
            description = "Comment not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "Post ID", example = "1", required = true)
            @Min(1) @PathVariable Long postId,
            @Parameter(description = "Comment ID", example = "1", required = true)
            @Min(1) @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        CommentResponse response = commentService.updateComment(postId, id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a comment",
            description = "Deletes a comment by ID. Requires admin role.")
    @ApiResponse(responseCode = "204", description = "Comment deleted")
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Comment not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "Post ID", example = "1", required = true)
            @Min(1) @PathVariable Long postId,
            @Parameter(description = "Comment ID", example = "1", required = true)
            @Min(1) @PathVariable Long id) {
        commentService.deleteComment(postId, id);
        return ResponseEntity.noContent().build();
    }
}
