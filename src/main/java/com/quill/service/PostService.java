package com.quill.service;

import com.quill.dto.request.PostRequest;
import com.quill.dto.response.PostResponse;
import com.quill.exception.CategoryNotFoundException;
import com.quill.exception.ForbiddenOperationException;
import com.quill.exception.PostNotFoundException;
import com.quill.exception.TagNotFoundException;
import com.quill.exception.UserNotFoundException;
import com.quill.mapper.PostMapper;
import com.quill.model.Category;
import com.quill.model.Post;
import com.quill.model.PostStatus;
import com.quill.model.Tag;
import com.quill.model.User;
import com.quill.repository.CategoryRepository;
import com.quill.repository.PostRepository;
import com.quill.repository.TagRepository;
import com.quill.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
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
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostMapper postMapper;
    private final SlugService slugService;

    public Page<PostResponse> findAllPosts(Pageable pageable) {
        log.debug(
                "Fetching published posts page: page={}, size={}, sort={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable).map(postMapper::toResponse);
    }

    public Page<PostResponse> searchPosts(String query, Pageable pageable) {
        log.debug("Searching posts: query='{}', page={}", query, pageable.getPageNumber());
        return postRepository.searchPosts(query, pageable).map(postMapper::toResponse);
    }

    public Page<PostResponse> findPostsByCategoryId(Long categoryId, Pageable pageable) {
        log.debug("Fetching published posts by category id={}, page={}", categoryId, pageable.getPageNumber());
        return postRepository
                .findByStatusAndCategoriesId(PostStatus.PUBLISHED, categoryId, pageable)
                .map(postMapper::toResponse);
    }

    public Page<PostResponse> findPostsByTagId(Long tagId, Pageable pageable) {
        log.debug("Fetching published posts by tag id={}, page={}", tagId, pageable.getPageNumber());
        return postRepository
                .findByStatusAndTagsId(PostStatus.PUBLISHED, tagId, pageable)
                .map(postMapper::toResponse);
    }

    public Page<PostResponse> findPostsByStatus(
            PostStatus status, Long categoryId, Long tagId, Pageable pageable, String username) {
        log.debug("Fetching posts by status={}, username={}", status, username);
        User author = findUserByUsername(username);
        if (categoryId != null) {
            return postRepository
                    .findByAuthorIdAndStatusAndCategoriesId(author.getId(), status, categoryId, pageable)
                    .map(postMapper::toResponse);
        }
        if (tagId != null) {
            return postRepository
                    .findByAuthorIdAndStatusAndTagsId(author.getId(), status, tagId, pageable)
                    .map(postMapper::toResponse);
        }
        return postRepository
                .findByAuthorIdAndStatus(author.getId(), status, pageable)
                .map(postMapper::toResponse);
    }

    public PostResponse findPostById(Long id, String username) {
        log.debug("Fetching post with id={}", id);
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        return checkPostVisibility(post, username);
    }

    public PostResponse findPostBySlug(String slug, String username) {
        log.debug("Fetching post with slug='{}'", slug);
        Post post = postRepository
                .findBySlug(slug)
                .orElseThrow(() -> new PostNotFoundException("Post not found with slug: " + slug));
        return checkPostVisibility(post, username);
    }

    @Transactional
    public PostResponse createPost(PostRequest request, String username) {
        log.info("Creating post: title='{}', username='{}'", request.title(), username);
        User author = findUserByUsername(username);
        Set<Category> categories = resolveCategories(request.categoryIds());
        Set<Tag> tags = resolveTags(request.tagIds());
        Post entity = postMapper.toEntity(request, author, categories, tags);
        entity.setSlug(slugService.toUniqueSlug(request.title(), "post", postRepository::existsBySlug));
        if (entity.getExcerpt() == null || entity.getExcerpt().isBlank()) {
            entity.setExcerpt(generateExcerpt(entity.getBody()));
        }
        applyStatus(entity, request.scheduledAt());
        Post saved = postRepository.save(entity);
        log.info("Created post with id={}, status={}", saved.getId(), saved.getStatus());
        return postMapper.toResponse(saved);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, String username) {
        log.info("Updating post with id={}", id);
        Post existing = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        String authorUsername = existing.getAuthor().getUsername();
        if (!authorUsername.equals(username)) {
            throw new ForbiddenOperationException("User '%s' is not the owner of post %d".formatted(username, id));
        }
        String oldSlug = existing.getSlug();
        String oldTitle = existing.getTitle();
        existing.setTitle(request.title());
        existing.setBody(request.body());
        if (!oldTitle.equals(request.title())) {
            existing.setSlug(slugService.toUniqueSlug(
                    request.title(), "post", s -> postRepository.existsBySlug(s) && !s.equals(oldSlug)));
        }
        existing.setCategories(resolveCategories(request.categoryIds()));
        existing.setTags(resolveTags(request.tagIds()));
        if (request.excerpt() == null || request.excerpt().isBlank()) {
            existing.setExcerpt(generateExcerpt(existing.getBody()));
        } else {
            existing.setExcerpt(request.excerpt());
        }
        applyStatusUpdate(existing, request.status(), request.scheduledAt());
        log.info("Updated post with id={}, status={}", id, existing.getStatus());
        return postMapper.toResponse(existing);
    }

    @Transactional
    public void deletePost(Long id) {
        log.info("Deleting post with id={}", id);
        if (!postRepository.existsById(id)) {
            throw new PostNotFoundException(id);
        }
        postRepository.deleteById(id);
        log.info("Deleted post with id={}", id);
    }

    private PostResponse checkPostVisibility(Post post, String username) {
        if (post.getStatus() == PostStatus.PUBLISHED) {
            return postMapper.toResponse(post);
        }
        if (post.getAuthor().getUsername().equals(username)) {
            return postMapper.toResponse(post);
        }
        throw new PostNotFoundException(post.getId());
    }

    private void applyStatus(Post entity, Instant scheduledAt) {
        if (entity.getStatus() == PostStatus.SCHEDULED) {
            if (scheduledAt == null) {
                throw new ForbiddenOperationException("scheduledAt is required when status is SCHEDULED");
            }
            if (scheduledAt.isBefore(Instant.now())) {
                throw new ForbiddenOperationException("scheduledAt must be in the future");
            }
            entity.setScheduledAt(scheduledAt);
        } else if (entity.getStatus() == PostStatus.PUBLISHED) {
            entity.setPublishedAt(Instant.now());
            entity.setScheduledAt(null);
        }
    }

    private void applyStatusUpdate(Post existing, PostStatus newStatus, Instant newScheduledAt) {
        if (newStatus == null) {
            return;
        }
        switch (newStatus) {
            case DRAFT -> {
                existing.setStatus(PostStatus.DRAFT);
                existing.setPublishedAt(null);
                existing.setScheduledAt(null);
            }
            case SCHEDULED -> {
                if (newScheduledAt == null) {
                    throw new ForbiddenOperationException("scheduledAt is required when status is SCHEDULED");
                }
                if (newScheduledAt.isBefore(Instant.now())) {
                    throw new ForbiddenOperationException("scheduledAt must be in the future");
                }
                existing.setStatus(PostStatus.SCHEDULED);
                existing.setScheduledAt(newScheduledAt);
                existing.setPublishedAt(null);
            }
            case PUBLISHED -> {
                existing.setStatus(PostStatus.PUBLISHED);
                if (existing.getPublishedAt() == null) {
                    existing.setPublishedAt(Instant.now());
                }
                existing.setScheduledAt(null);
            }
        }
    }

    private User findUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Set.of();
        }
        return categoryIds.stream()
                .map(cid -> categoryRepository.findById(cid).orElseThrow(() -> new CategoryNotFoundException(cid)))
                .collect(Collectors.toSet());
    }

    private String generateExcerpt(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String cleaned = body.strip();
        if (cleaned.length() <= 250) {
            return cleaned;
        }
        int lastSpace = cleaned.lastIndexOf(' ', 250);
        if (lastSpace == -1) {
            return cleaned.substring(0, 250) + "...";
        }
        return cleaned.substring(0, lastSpace) + "...";
    }

    private Set<Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Set.of();
        }
        return tagIds.stream()
                .map(tid -> tagRepository.findById(tid).orElseThrow(() -> new TagNotFoundException(tid)))
                .collect(Collectors.toSet());
    }
}
