package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService")
class PostServiceTest {

    private static final Long POST_ID = 10L;
    private static final Long MISSING_POST_ID = 99L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long CATEGORY_ID = 5L;
    private static final Long TAG_ID = 7L;
    private static final String USERNAME = "alice";

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private User author;
    private Post post;
    private PostRequest request;
    private PostResponse response;
    private Category category;
    private Tag tag;

    @BeforeEach
    void setUp() {
        author = User.builder().id(AUTHOR_ID).username("alice").build();
        category = Category.builder().id(CATEGORY_ID).name("Tech").build();
        tag = Tag.builder().id(TAG_ID).name("java").build();
        post = Post.builder()
                .id(POST_ID)
                .title("Title")
                .body("Body")
                .author(author)
                .categories(Set.of(category))
                .tags(Set.of(tag))
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        request = new PostRequest("New title", "New body", Set.of(CATEGORY_ID), Set.of(TAG_ID));
        response = new PostResponse(
                POST_ID,
                "New title",
                "New body",
                AUTHOR_ID,
                Set.of(CATEGORY_ID),
                Set.of(TAG_ID),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Nested
    @DisplayName("findAllPosts")
    class FindAllPosts {

        @Test
        @DisplayName("delegates to repository and maps each post via the mapper")
        void delegatesToRepositoryAndMaps() {
            var pageable = PageRequest.of(0, 10);
            var otherResponse = new PostResponse(11L, "Other", "...", AUTHOR_ID, Set.of(1L), Set.of(), null, null);
            var page = new PageImpl<>(List.of(post), pageable, 1);
            when(postRepository.findAll(pageable)).thenReturn(page);
            when(postMapper.toResponse(post)).thenReturn(response);

            Page<PostResponse> result = postService.findAllPosts(pageable);

            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(postRepository).findAll(pageable);
        }

        @Test
        @DisplayName("returns an empty page when repository has no posts")
        void returnsEmptyPage() {
            var pageable = PageRequest.of(0, 10);
            var empty = new PageImpl<>(List.<Post>of(), pageable, 0);
            when(postRepository.findAll(pageable)).thenReturn(empty);

            Page<PostResponse> result = postService.findAllPosts(pageable);

            assertThat(result.getContent()).isEmpty();
            verify(postMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("findPostsByCategoryId")
    class FindPostsByCategoryId {

        @Test
        @DisplayName("delegates to repository and maps")
        void delegates() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(post), pageable, 1);
            when(postRepository.findByCategoriesId(CATEGORY_ID, pageable)).thenReturn(page);
            when(postMapper.toResponse(post)).thenReturn(response);

            Page<PostResponse> result = postService.findPostsByCategoryId(CATEGORY_ID, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository).findByCategoriesId(CATEGORY_ID, pageable);
        }
    }

    @Nested
    @DisplayName("findPostsByTagId")
    class FindPostsByTagId {

        @Test
        @DisplayName("delegates to repository and maps")
        void delegates() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(post), pageable, 1);
            when(postRepository.findByTagsId(TAG_ID, pageable)).thenReturn(page);
            when(postMapper.toResponse(post)).thenReturn(response);

            Page<PostResponse> result = postService.findPostsByTagId(TAG_ID, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository).findByTagsId(TAG_ID, pageable);
        }
    }

    @Nested
    @DisplayName("findPostById")
    class FindPostById {

        @Test
        @DisplayName("returns a mapped response when the post exists")
        void returnsMappedResponse() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.findPostById(POST_ID);

            assertThat(result).isEqualTo(response);
            verify(postRepository).findById(POST_ID);
            verify(postMapper).toResponse(post);
        }

        @Test
        @DisplayName("throws PostNotFoundException with the missing id when the post does not exist")
        void throwsWhenMissing() {
            when(postRepository.findById(MISSING_POST_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(PostNotFoundException.class, () -> postService.findPostById(MISSING_POST_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(postMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("resolves categories, tags, and author; persists; and returns the mapped response")
        void createsAndReturns() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
            when(postMapper.toEntity(request, author, Set.of(category), Set.of(tag)))
                    .thenReturn(post);
            when(postRepository.save(post)).thenReturn(post);
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.createPost(request, USERNAME);

            assertThat(result).isEqualTo(response);
            verify(userRepository).findByUsername(USERNAME);
            verify(categoryRepository).findById(CATEGORY_ID);
            verify(tagRepository).findById(TAG_ID);
            verify(postMapper).toEntity(request, author, Set.of(category), Set.of(tag));
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("creates with no tags when tagIds is empty")
        void createsWithNoTags() {
            var requestNoTags = new PostRequest("Title", "Body", Set.of(CATEGORY_ID), Set.of());
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(postMapper.toEntity(requestNoTags, author, Set.of(category), Set.of()))
                    .thenReturn(post);
            when(postRepository.save(post)).thenReturn(post);
            when(postMapper.toResponse(post)).thenReturn(response);

            postService.createPost(requestNoTags, USERNAME);

            verify(tagRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when the username does not exist")
        void throwsWhenAuthorMissing() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            var thrown = assertThrows(UserNotFoundException.class, () -> postService.createPost(request, USERNAME));
            assertThat(thrown).hasMessageContaining(USERNAME);

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when a category does not exist")
        void throwsWhenCategoryMissing() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThrows(CategoryNotFoundException.class, () -> postService.createPost(request, USERNAME));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TagNotFoundException when a tag does not exist")
        void throwsWhenTagMissing() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.empty());

            assertThrows(TagNotFoundException.class, () -> postService.createPost(request, USERNAME));
            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("allows the owner to update and returns the mapped response")
        void ownerCanUpdate() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.updatePost(POST_ID, request, USERNAME, false);

            assertThat(result).isEqualTo(response);
            assertThat(post.getTitle()).isEqualTo(request.title());
            assertThat(post.getBody()).isEqualTo(request.body());
            assertThat(post.getCategories()).containsExactly(category);
            assertThat(post.getTags()).containsExactly(tag);
        }

        @Test
        @DisplayName("allows an admin to update any post")
        void adminCanUpdate() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.updatePost(POST_ID, request, "other", true);

            assertThat(result).isEqualTo(response);
            assertThat(post.getTitle()).isEqualTo(request.title());
            assertThat(post.getBody()).isEqualTo(request.body());
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when a non-owner non-admin tries to update")
        void throwsWhenNotOwner() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

            var thrown = assertThrows(
                    ForbiddenOperationException.class, () -> postService.updatePost(POST_ID, request, "other", false));
            assertThat(thrown).hasMessageContaining("other").hasMessageContaining(String.valueOf(POST_ID));
        }

        @Test
        @DisplayName("throws PostNotFoundException with the missing id when the post does not exist")
        void throwsWhenPostMissing() {
            when(postRepository.findById(MISSING_POST_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.updatePost(MISSING_POST_ID, request, USERNAME, false));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("deletes the post when it exists")
        void deletesExisting() {
            when(postRepository.existsById(POST_ID)).thenReturn(true);

            postService.deletePost(POST_ID);

            verify(postRepository).existsById(POST_ID);
            verify(postRepository).deleteById(POST_ID);
        }

        @Test
        @DisplayName("throws PostNotFoundException with the missing id when the post does not exist")
        void throwsWhenMissing() {
            when(postRepository.existsById(MISSING_POST_ID)).thenReturn(false);

            var thrown = assertThrows(PostNotFoundException.class, () -> postService.deletePost(MISSING_POST_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(postRepository, never()).deleteById(MISSING_POST_ID);
        }
    }
}
