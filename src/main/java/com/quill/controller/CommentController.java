package com.quill.controller;

import com.quill.dto.request.CommentRequest;
import com.quill.dto.response.CommentResponse;
import com.quill.service.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<Page<CommentResponse>> findAllCommentsByPostId(
            @Min(1) @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(commentService.findAllCommentsByPostId(postId, pageable));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Min(1) @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        CommentResponse response = commentService.createComment(request, postId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @Min(1) @PathVariable Long postId,
            @Min(1) @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        CommentResponse response = commentService.updateComment(postId, id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@Min(1) @PathVariable Long postId, @Min(1) @PathVariable Long id) {
        commentService.deleteComment(postId, id);
        return ResponseEntity.noContent().build();
    }
}
