package com.quill.controller;

import com.quill.dto.CommentRequest;
import com.quill.dto.CommentResponse;
import com.quill.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> findAllCommentsByPostId(
            @PathVariable Long postId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(commentService.findAllCommentsByPostId(postId, pageable));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId, @Valid @RequestBody CommentRequest request, Authentication authentication) {
        CommentResponse response = commentService.createComment(request, postId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
