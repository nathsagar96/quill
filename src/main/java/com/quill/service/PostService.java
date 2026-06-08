package com.quill.service;

import com.quill.dto.PostRequest;
import com.quill.dto.PostResponse;
import com.quill.exception.CategoryNotFoundException;
import com.quill.exception.ForbiddenOperationException;
import com.quill.exception.PostNotFoundException;
import com.quill.exception.TagNotFoundException;
import com.quill.exception.UserNotFoundException;
import com.quill.mapper.PostMapper;
import com.quill.model.Category;
import com.quill.model.Post;
import com.quill.model.Tag;
import com.quill.model.User;
import com.quill.repository.CategoryRepository;
import com.quill.repository.PostRepository;
import com.quill.repository.TagRepository;
import com.quill.repository.UserRepository;
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

    public Page<PostResponse> findAllPosts(Pageable pageable) {
        log.debug(
                "Fetching posts page: page={}, size={}, sort={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());
        return postRepository.findAll(pageable).map(postMapper::toResponse);
    }

    public Page<PostResponse> findPostsByCategoryId(Long categoryId, Pageable pageable) {
        log.debug("Fetching posts by category id={}, page={}", categoryId, pageable.getPageNumber());
        return postRepository.findByCategoriesId(categoryId, pageable).map(postMapper::toResponse);
    }

    public Page<PostResponse> findPostsByTagId(Long tagId, Pageable pageable) {
        log.debug("Fetching posts by tag id={}, page={}", tagId, pageable.getPageNumber());
        return postRepository.findByTagsId(tagId, pageable).map(postMapper::toResponse);
    }

    public PostResponse findPostById(Long id) {
        log.debug("Fetching post with id={}", id);
        return postRepository.findById(id).map(postMapper::toResponse).orElseThrow(() -> new PostNotFoundException(id));
    }

    @Transactional
    public PostResponse createPost(PostRequest request, String username) {
        log.info("Creating post: title='{}', username='{}'", request.title(), username);
        User author = findUserByUsername(username);
        Set<Category> categories = resolveCategories(request.categoryIds());
        Set<Tag> tags = resolveTags(request.tagIds());
        Post entity = postMapper.toEntity(request, author, categories, tags);
        if (entity.getExcerpt() == null || entity.getExcerpt().isBlank()) {
            entity.setExcerpt(generateExcerpt(entity.getBody()));
        }
        Post saved = postRepository.save(entity);
        log.info("Created post with id={}", saved.getId());
        return postMapper.toResponse(saved);
    }

    @Transactional
    public PostResponse updatePost(Long id, PostRequest request, String username, boolean isAdmin) {
        log.info("Updating post with id={}", id);
        Post existing = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        String authorUsername = existing.getAuthor().getUsername();
        if (!authorUsername.equals(username) && !isAdmin) {
            throw new ForbiddenOperationException("User '%s' is not the owner of post %d".formatted(username, id));
        }
        existing.setTitle(request.title());
        existing.setBody(request.body());
        existing.setCategories(resolveCategories(request.categoryIds()));
        existing.setTags(resolveTags(request.tagIds()));
        if (request.excerpt() == null || request.excerpt().isBlank()) {
            existing.setExcerpt(generateExcerpt(existing.getBody()));
        } else {
            existing.setExcerpt(request.excerpt());
        }
        log.info("Updated post with id={}", id);
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

    private User findUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        return categoryIds.stream()
                .map(cid -> categoryRepository.findById(cid).orElseThrow(() -> new CategoryNotFoundException(cid)))
                .collect(Collectors.toSet());
    }

    private String generateExcerpt(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String cleaned = body.strip();
        if (cleaned.length() <= 150) {
            return cleaned;
        }
        int lastSpace = cleaned.lastIndexOf(' ', 150);
        if (lastSpace == -1) {
            return cleaned.substring(0, 150) + "...";
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
