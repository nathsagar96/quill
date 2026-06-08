package com.quill.service;

import com.quill.dto.PostRequest;
import com.quill.dto.PostResponse;
import com.quill.exception.PostNotFoundException;
import com.quill.exception.UserNotFoundException;
import com.quill.mapper.PostMapper;
import com.quill.model.Post;
import com.quill.model.User;
import com.quill.repository.PostRepository;
import com.quill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    public Page<PostResponse> findAllPosts(Pageable pageable) {
        log.debug(
                "Fetching posts page: page={}, size={}, sort={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());
        return postRepository.findAll(pageable).map(postMapper::toResponse);
    }

    public PostResponse findPostById(Long id) {
        log.debug("Fetching post with id={}", id);
        return postRepository.findById(id).map(postMapper::toResponse).orElseThrow(() -> new PostNotFoundException(id));
    }

    @Transactional
    public PostResponse createPost(PostRequest request, String username) {
        log.info("Creating post: title='{}', username='{}'", request.title(), username);
        User author = findUserByUsername(username);
        Post entity = postMapper.toEntity(request, author);
        Post saved = postRepository.save(entity);
        log.info("Created post with id={}", saved.getId());
        return postMapper.toResponse(saved);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        log.info("Updating post with id={}", id);
        Post existing = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        existing.setTitle(request.title());
        existing.setBody(request.body());
        log.info("Updated post with id={}", id);
        return postMapper.toResponse(existing);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePost(Long id) {
        log.info("Deleting post with id={}", id);
        if (!postRepository.existsById(id)) {
            throw new PostNotFoundException(id);
        }
        postRepository.deleteById(id);
        log.info("Deleted post with id={}", id);
    }

    private User findUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }
}
