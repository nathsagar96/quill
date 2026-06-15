package com.quill.service;

import com.quill.dto.request.CommentRequest;
import com.quill.dto.response.CommentResponse;
import com.quill.exception.CommentNotFoundException;
import com.quill.exception.ForbiddenOperationException;
import com.quill.exception.PostNotFoundException;
import com.quill.model.PostStatus;
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
        findPublishedPostById(postId);
        return commentRepository.findByPostId(postId, pageable).map(commentMapper::toResponse);
    }

    @Transactional
    public CommentResponse createComment(CommentRequest request, Long postId, String username) {
        log.info("Creating comment: postId={}, username='{}'", postId, username);
        Post post = findPublishedPostById(postId);
        User author = findUserByUsername(username);
        Comment entity = commentMapper.toEntity(request, post, author);
        Comment saved = commentRepository.save(entity);
        log.info("Created comment with id={}", saved.getId());
        return commentMapper.toResponse(saved);
    }

    @Transactional
    public CommentResponse updateComment(Long postId, Long id, CommentRequest request, String username) {
        log.info("Updating comment id={} on post id={} by user '{}'", id, postId, username);
        Comment comment = findCommentByIdAndPostId(id, postId);
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new ForbiddenOperationException("User '%s' is not the owner of comment %d".formatted(username, id));
        }
        comment.setBody(request.body());
        log.info("Updated comment id={}", id);
        return commentMapper.toResponse(comment);
    }

    @Transactional
    public void deleteComment(Long postId, Long id) {
        log.info("Deleting comment id={} on post id={}", id, postId);
        Comment comment = findCommentByIdAndPostId(id, postId);
        commentRepository.delete(comment);
        log.info("Deleted comment id={}", id);
    }

    private Comment findCommentByIdAndPostId(Long id, Long postId) {
        return commentRepository.findByIdAndPostId(id, postId).orElseThrow(() -> new CommentNotFoundException(id));
    }

    private Post findPublishedPostById(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new PostNotFoundException(id);
        }
        return post;
    }

    private User findUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }
}
