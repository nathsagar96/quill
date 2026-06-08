package com.quill.service;

import com.quill.dto.CommentRequest;
import com.quill.dto.CommentResponse;
import com.quill.exception.CommentNotFoundException;
import com.quill.exception.PostNotFoundException;
import com.quill.exception.UserNotFoundException;
import com.quill.mapper.CommentMapper;
import com.quill.model.Comment;
import com.quill.model.Post;
import com.quill.model.User;
import com.quill.repository.CommentRepository;
import com.quill.repository.PostRepository;
import com.quill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    public Page<CommentResponse> findAllCommentsByPostId(Long postId, Pageable pageable) {
        log.debug(
                "Fetching comments for post id={}, page={}, size={}",
                postId,
                pageable.getPageNumber(),
                pageable.getPageSize());
        return commentRepository.findByPostId(postId, pageable).map(commentMapper::toResponse);
    }

    public CommentResponse findCommentById(Long id) {
        log.debug("Fetching comment with id={}", id);
        return commentRepository
                .findById(id)
                .map(commentMapper::toResponse)
                .orElseThrow(() -> new CommentNotFoundException(id));
    }

    @Transactional
    public CommentResponse createComment(CommentRequest request, Long postId, String username) {
        log.info("Creating comment: postId={}, username='{}'", postId, username);
        Post post = findPostById(postId);
        User author = findUserByUsername(username);
        Comment entity = commentMapper.toEntity(request, post, author);
        Comment saved = commentRepository.save(entity);
        log.info("Created comment with id={}", saved.getId());
        return commentMapper.toResponse(saved);
    }

    private Post findPostById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
    }

    private User findUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }
}
