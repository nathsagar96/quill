package com.quill.controller;

import com.quill.dto.request.PostRequest;
import com.quill.dto.response.PostResponse;
import com.quill.service.PostService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Validated
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> findAllPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (categoryId != null) {
            return ResponseEntity.ok(postService.findPostsByCategoryId(categoryId, pageable));
        }
        if (tagId != null) {
            return ResponseEntity.ok(postService.findPostsByTagId(tagId, pageable));
        }
        return ResponseEntity.ok(postService.findAllPosts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> findPostById(@Min(1) @PathVariable Long id) {
        return ResponseEntity.ok(postService.findPostById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PostResponse> findPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postService.findPostBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request, Authentication authentication) {
        PostResponse response = postService.createPost(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @Min(1) @PathVariable Long id, @Valid @RequestBody PostRequest request, Authentication authentication) {
        PostResponse response = postService.updatePost(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@Min(1) @PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
