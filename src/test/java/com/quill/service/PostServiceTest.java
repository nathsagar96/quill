package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.dto.request.PostRequest;
import com.quill.dto.response.AuthorResponse;
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

    @Mock
    private SlugService slugService;

    @InjectMocks
    private PostService postService;

    private User author;
    private Post publishedPost;
    private Post draftPost;
    private PostRequest request;
    private PostResponse response;
    private Category category;
    private Tag tag;
    private final Instant now = Instant.parse("2024-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        author = User.builder().id(AUTHOR_ID).username("alice").build();
        category = Category.builder().id(CATEGORY_ID).name("Tech").build();
        tag = Tag.builder().id(TAG_ID).name("java").build();
        publishedPost = Post.builder()
                .id(POST_ID)
                .title("Title")
                .body("Body")
                .slug("test-post")
                .author(author)
                .categories(Set.of(category))
                .tags(Set.of(tag))
                .status(PostStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        draftPost = Post.builder()
                .id(POST_ID)
                .title("Title")
                .body("Body")
                .slug("test-post")
                .author(author)
                .categories(Set.of(category))
                .tags(Set.of(tag))
                .status(PostStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        request = new PostRequest("New title", "New body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), null, null);
        response = new PostResponse(
                POST_ID,
                "New title",
                "New body",
                null,
                "new-title",
                new AuthorResponse(AUTHOR_ID, null, null, null, null),
                Set.of(CATEGORY_ID),
                Set.of(TAG_ID),
                PostStatus.PUBLISHED,
                null,
                null,
                now,
                now);
    }

    @Nested
    @DisplayName("findAllPosts")
    class FindAllPosts {

        @Test
        @DisplayName("returns only published posts")
        void delegatesToRepositoryAndMaps() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(publishedPost), pageable, 1);
            when(postRepository.findByStatus(PostStatus.PUBLISHED, pageable)).thenReturn(page);
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            Page<PostResponse> result = postService.findAllPosts(pageable);

            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(postRepository).findByStatus(PostStatus.PUBLISHED, pageable);
        }

        @Test
        @DisplayName("returns an empty page when repository has no published posts")
        void returnsEmptyPage() {
            var pageable = PageRequest.of(0, 10);
            var empty = new PageImpl<>(List.<Post>of(), pageable, 0);
            when(postRepository.findByStatus(PostStatus.PUBLISHED, pageable)).thenReturn(empty);

            Page<PostResponse> result = postService.findAllPosts(pageable);

            assertThat(result.getContent()).isEmpty();
            verify(postMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("searchPosts")
    class SearchPosts {

        @Test
        @DisplayName("delegates to repository and maps results")
        void delegatesAndMaps() {
            var query = "java spring";
            var pageable = PageRequest.of(0, 10);
            var ids = List.of(publishedPost.getId());
            var idPage = new PageImpl<>(ids, pageable, 1);
            when(postRepository.searchPostIds(query, pageable)).thenReturn(idPage);
            when(postRepository.findByIdIn(ids)).thenReturn(List.of(publishedPost));
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            Page<PostResponse> result = postService.searchPosts(query, pageable);

            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(postRepository).searchPostIds(query, pageable);
            verify(postRepository).findByIdIn(ids);
            verify(postMapper).toResponse(publishedPost);
        }

        @Test
        @DisplayName("returns empty page when no posts match")
        void returnsEmptyPage() {
            var query = "nonexistent";
            var pageable = PageRequest.of(0, 10);
            var empty = new PageImpl<>(List.<Long>of(), pageable, 0);
            when(postRepository.searchPostIds(query, pageable)).thenReturn(empty);

            Page<PostResponse> result = postService.searchPosts(query, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(postMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("findPostsByCategoryId")
    class FindPostsByCategoryId {

        @Test
        @DisplayName("returns only published posts by category")
        void delegates() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(publishedPost), pageable, 1);
            when(postRepository.findByStatusAndCategoriesId(PostStatus.PUBLISHED, CATEGORY_ID, pageable))
                    .thenReturn(page);
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            Page<PostResponse> result = postService.findPostsByCategoryId(CATEGORY_ID, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository).findByStatusAndCategoriesId(PostStatus.PUBLISHED, CATEGORY_ID, pageable);
        }
    }

    @Nested
    @DisplayName("findPostsByTagId")
    class FindPostsByTagId {

        @Test
        @DisplayName("returns only published posts by tag")
        void delegates() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(publishedPost), pageable, 1);
            when(postRepository.findByStatusAndTagsId(PostStatus.PUBLISHED, TAG_ID, pageable))
                    .thenReturn(page);
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            Page<PostResponse> result = postService.findPostsByTagId(TAG_ID, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository).findByStatusAndTagsId(PostStatus.PUBLISHED, TAG_ID, pageable);
        }
    }

    @Nested
    @DisplayName("findPostsByStatus")
    class FindPostsByStatus {

        @Test
        @DisplayName("returns posts filtered by author and status")
        void byStatus() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(draftPost), pageable, 1);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(postRepository.findByAuthorIdAndStatus(AUTHOR_ID, PostStatus.DRAFT, pageable))
                    .thenReturn(page);
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            Page<PostResponse> result = postService.findPostsByStatus(PostStatus.DRAFT, null, null, pageable, USERNAME);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository).findByAuthorIdAndStatus(AUTHOR_ID, PostStatus.DRAFT, pageable);
        }

        @Test
        @DisplayName("returns posts filtered by author, status, and category")
        void byStatusAndCategory() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(draftPost), pageable, 1);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(postRepository.findByAuthorIdAndStatusAndCategoriesId(
                            AUTHOR_ID, PostStatus.DRAFT, CATEGORY_ID, pageable))
                    .thenReturn(page);
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            Page<PostResponse> result =
                    postService.findPostsByStatus(PostStatus.DRAFT, CATEGORY_ID, null, pageable, USERNAME);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository)
                    .findByAuthorIdAndStatusAndCategoriesId(AUTHOR_ID, PostStatus.DRAFT, CATEGORY_ID, pageable);
        }
    }

    @Nested
    @DisplayName("findMyPosts")
    class FindMyPosts {

        @Test
        @DisplayName("returns all posts for the authenticated user")
        void allPosts() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(draftPost, publishedPost), pageable, 2);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(postRepository.findByAuthorId(AUTHOR_ID, pageable)).thenReturn(page);
            when(postMapper.toResponse(draftPost)).thenReturn(response);
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            Page<PostResponse> result = postService.findMyPosts(null, null, pageable, USERNAME);

            assertThat(result.getContent()).hasSize(2);
            verify(postRepository).findByAuthorId(AUTHOR_ID, pageable);
        }

        @Test
        @DisplayName("returns posts filtered by user and category")
        void byCategory() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(draftPost), pageable, 1);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(postRepository.findByAuthorIdAndCategoriesId(AUTHOR_ID, CATEGORY_ID, pageable))
                    .thenReturn(page);
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            Page<PostResponse> result = postService.findMyPosts(CATEGORY_ID, null, pageable, USERNAME);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository).findByAuthorIdAndCategoriesId(AUTHOR_ID, CATEGORY_ID, pageable);
        }

        @Test
        @DisplayName("returns posts filtered by user and tag")
        void byTag() {
            var pageable = PageRequest.of(0, 10);
            var page = new PageImpl<>(List.of(draftPost), pageable, 1);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(postRepository.findByAuthorIdAndTagsId(AUTHOR_ID, TAG_ID, pageable))
                    .thenReturn(page);
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            Page<PostResponse> result = postService.findMyPosts(null, TAG_ID, pageable, USERNAME);

            assertThat(result.getContent()).containsExactly(response);
            verify(postRepository).findByAuthorIdAndTagsId(AUTHOR_ID, TAG_ID, pageable);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void userNotFound() {
            var pageable = PageRequest.of(0, 10);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> postService.findMyPosts(null, null, pageable, USERNAME));
        }
    }

    @Nested
    @DisplayName("findPostById")
    class FindPostById {

        @Test
        @DisplayName("returns a published post to any user")
        void returnsPublishedToAnyone() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(publishedPost));
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            PostResponse result = postService.findPostById(POST_ID, null);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("returns a draft post to its author")
        void returnsDraftToAuthor() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(draftPost));
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            PostResponse result = postService.findPostById(POST_ID, USERNAME);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("throws PostNotFoundException for a draft post when user is not the author")
        void throwsForDraftWhenNotAuthor() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(draftPost));

            assertThrows(PostNotFoundException.class, () -> postService.findPostById(POST_ID, "other"));
        }

        @Test
        @DisplayName("throws PostNotFoundException for a draft post when unauthenticated")
        void throwsForDraftWhenAnonymous() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(draftPost));

            assertThrows(PostNotFoundException.class, () -> postService.findPostById(POST_ID, null));
        }

        @Test
        @DisplayName("throws PostNotFoundException when the post does not exist")
        void throwsWhenMissing() {
            when(postRepository.findById(MISSING_POST_ID)).thenReturn(Optional.empty());

            var thrown =
                    assertThrows(PostNotFoundException.class, () -> postService.findPostById(MISSING_POST_ID, null));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));
            verify(postMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("findPostBySlug")
    class FindPostBySlug {

        private static final String SLUG = "my-post";

        @Test
        @DisplayName("returns a published post to any user")
        void returnsPublishedToAnyone() {
            when(postRepository.findBySlug(SLUG)).thenReturn(Optional.of(publishedPost));
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            PostResponse result = postService.findPostBySlug(SLUG, null);

            assertThat(result).isEqualTo(response);
            verify(postRepository).findBySlug(SLUG);
        }

        @Test
        @DisplayName("returns a draft post to its author")
        void returnsDraftToAuthor() {
            when(postRepository.findBySlug(SLUG)).thenReturn(Optional.of(draftPost));
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            PostResponse result = postService.findPostBySlug(SLUG, USERNAME);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("throws PostNotFoundException for a draft when unauthenticated")
        void throwsForDraftWhenAnonymous() {
            when(postRepository.findBySlug(SLUG)).thenReturn(Optional.of(draftPost));

            assertThrows(PostNotFoundException.class, () -> postService.findPostBySlug(SLUG, null));
        }

        @Test
        @DisplayName("throws PostNotFoundException when the slug does not exist")
        void throwsWhenMissing() {
            when(postRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

            var thrown = assertThrows(PostNotFoundException.class, () -> postService.findPostBySlug(SLUG, null));
            assertThat(thrown).hasMessageContaining(SLUG);
            verify(postMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("creates a draft post by default")
        void createsDraftByDefault() {
            var draftPost = Post.builder()
                    .id(POST_ID)
                    .title("New title")
                    .body("New body")
                    .slug("new-title")
                    .author(author)
                    .categories(Set.of(category))
                    .tags(Set.of(tag))
                    .status(PostStatus.DRAFT)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            when(slugService.toUniqueSlug(eq("New title"), eq("post"), any())).thenReturn("new-title");
            when(postMapper.toEntity(request, author, Set.of(category), Set.of(tag)))
                    .thenReturn(draftPost);
            when(postRepository.save(draftPost)).thenReturn(draftPost);
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            PostResponse result = postService.createPost(request, USERNAME);

            assertThat(result).isEqualTo(response);
            verify(postRepository).save(draftPost);
        }

        @Test
        @DisplayName("creates a published post when status is PUBLISHED")
        void createsPublished() {
            var pubRequest = new PostRequest(
                    "Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), PostStatus.PUBLISHED, null);
            var pubPost = Post.builder()
                    .id(POST_ID)
                    .title("Title")
                    .body("Body")
                    .slug("title")
                    .author(author)
                    .categories(Set.of(category))
                    .tags(Set.of(tag))
                    .status(PostStatus.PUBLISHED)
                    .publishedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            when(slugService.toUniqueSlug(eq("Title"), eq("post"), any())).thenReturn("title");
            when(postMapper.toEntity(pubRequest, author, Set.of(category), Set.of(tag)))
                    .thenReturn(pubPost);
            when(postRepository.save(pubPost)).thenReturn(pubPost);
            when(postMapper.toResponse(pubPost)).thenReturn(response);

            postService.createPost(pubRequest, USERNAME);

            verify(postRepository).save(pubPost);
            assertThat(pubPost.getStatus()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(pubPost.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when SCHEDULED without scheduledAt")
        void throwsWhenScheduledMissingTime() {
            var req = new PostRequest(
                    "Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), PostStatus.SCHEDULED, null);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            var schPost = Post.builder()
                    .id(POST_ID)
                    .title("Title")
                    .body("Body")
                    .slug("title")
                    .author(author)
                    .categories(Set.of(category))
                    .tags(Set.of(tag))
                    .status(PostStatus.SCHEDULED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            when(slugService.toUniqueSlug(eq("Title"), eq("post"), any())).thenReturn("title");
            when(postMapper.toEntity(req, author, Set.of(category), Set.of(tag)))
                    .thenReturn(schPost);

            assertThrows(ForbiddenOperationException.class, () -> postService.createPost(req, USERNAME));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when SCHEDULED with past scheduledAt")
        void throwsWhenScheduledInPast() {
            var req = new PostRequest(
                    "Title",
                    "Body",
                    null,
                    Set.of(CATEGORY_ID),
                    Set.of(TAG_ID),
                    PostStatus.SCHEDULED,
                    Instant.parse("2020-01-01T00:00:00Z"));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            var schPost = Post.builder()
                    .id(POST_ID)
                    .title("Title")
                    .body("Body")
                    .slug("title")
                    .author(author)
                    .categories(Set.of(category))
                    .tags(Set.of(tag))
                    .status(PostStatus.SCHEDULED)
                    .scheduledAt(Instant.parse("2020-01-01T00:00:00Z"))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            when(slugService.toUniqueSlug(eq("Title"), eq("post"), any())).thenReturn("title");
            when(postMapper.toEntity(req, author, Set.of(category), Set.of(tag)))
                    .thenReturn(schPost);

            assertThrows(ForbiddenOperationException.class, () -> postService.createPost(req, USERNAME));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("resolves categories, tags, and author; persists; and returns the mapped response")
        void createsAndReturns() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            when(slugService.toUniqueSlug(eq("New title"), eq("post"), any())).thenReturn("new-title");
            when(postMapper.toEntity(request, author, Set.of(category), Set.of(tag)))
                    .thenReturn(publishedPost);
            when(postRepository.save(publishedPost)).thenReturn(publishedPost);
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            PostResponse result = postService.createPost(request, USERNAME);

            assertThat(result).isEqualTo(response);
            verify(userRepository).findByUsername(USERNAME);
            verify(categoryRepository).findAllByIdIn(Set.of(CATEGORY_ID));
            verify(tagRepository).findAllByIdIn(Set.of(TAG_ID));
            verify(postMapper).toEntity(request, author, Set.of(category), Set.of(tag));
            verify(postRepository).save(publishedPost);
        }

        @Test
        @DisplayName("creates with no tags when tagIds is empty")
        void createsWithNoTags() {
            var requestNoTags = new PostRequest("Title", "Body", null, Set.of(CATEGORY_ID), Set.of(), null, null);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(slugService.toUniqueSlug(eq("Title"), eq("post"), any())).thenReturn("title");
            when(postMapper.toEntity(requestNoTags, author, Set.of(category), Set.of()))
                    .thenReturn(publishedPost);
            when(postRepository.save(publishedPost)).thenReturn(publishedPost);
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            postService.createPost(requestNoTags, USERNAME);

            verify(tagRepository, never()).findAllByIdIn(any());
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
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of());

            assertThrows(CategoryNotFoundException.class, () -> postService.createPost(request, USERNAME));
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TagNotFoundException when a tag does not exist")
        void throwsWhenTagMissing() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of());

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
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(publishedPost));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            when(slugService.toUniqueSlug(eq("New title"), eq("post"), any())).thenReturn("new-title");
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            PostResponse result = postService.updatePost(POST_ID, request, USERNAME);

            assertThat(result).isEqualTo(response);
            assertThat(publishedPost.getTitle()).isEqualTo(request.title());
            assertThat(publishedPost.getBody()).isEqualTo(request.body());
            assertThat(publishedPost.getSlug()).isEqualTo("new-title");
            assertThat(publishedPost.getCategories()).containsExactly(category);
            assertThat(publishedPost.getTags()).containsExactly(tag);
        }

        @Test
        @DisplayName("allows owner to change status from DRAFT to PUBLISHED")
        void canPublish() {
            var pubRequest = new PostRequest(
                    "Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), PostStatus.PUBLISHED, null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(draftPost));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            postService.updatePost(POST_ID, pubRequest, USERNAME);

            assertThat(draftPost.getStatus()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(draftPost.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("allows owner to schedule a post")
        void canSchedule() {
            var future = Instant.parse("2027-01-01T00:00:00Z");
            var schRequest = new PostRequest(
                    "Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), PostStatus.SCHEDULED, future);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(draftPost));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            when(postMapper.toResponse(draftPost)).thenReturn(response);

            postService.updatePost(POST_ID, schRequest, USERNAME);

            assertThat(draftPost.getStatus()).isEqualTo(PostStatus.SCHEDULED);
            assertThat(draftPost.getScheduledAt()).isEqualTo(future);
        }

        @Test
        @DisplayName("rejects SCHEDULED without scheduledAt")
        void rejectsScheduleWithoutTime() {
            var schRequest = new PostRequest(
                    "Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), PostStatus.SCHEDULED, null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(draftPost));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));

            assertThrows(
                    ForbiddenOperationException.class, () -> postService.updatePost(POST_ID, schRequest, USERNAME));
        }

        @Test
        @DisplayName("allows owner to draft a published post")
        void canUnpublish() {
            var draftRequest =
                    new PostRequest("Title", "Body", null, Set.of(CATEGORY_ID), Set.of(TAG_ID), PostStatus.DRAFT, null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(publishedPost));
            when(categoryRepository.findAllByIdIn(Set.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(tagRepository.findAllByIdIn(Set.of(TAG_ID))).thenReturn(List.of(tag));
            when(postMapper.toResponse(publishedPost)).thenReturn(response);

            postService.updatePost(POST_ID, draftRequest, USERNAME);

            assertThat(publishedPost.getStatus()).isEqualTo(PostStatus.DRAFT);
            assertThat(publishedPost.getPublishedAt()).isNull();
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when a non-owner tries to update")
        void throwsWhenNotOwner() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(publishedPost));

            var thrown = assertThrows(
                    ForbiddenOperationException.class, () -> postService.updatePost(POST_ID, request, "other"));
            assertThat(thrown).hasMessageContaining("other").hasMessageContaining(String.valueOf(POST_ID));
        }

        @Test
        @DisplayName("throws PostNotFoundException with the missing id when the post does not exist")
        void throwsWhenPostMissing() {
            when(postRepository.findById(MISSING_POST_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    PostNotFoundException.class, () -> postService.updatePost(MISSING_POST_ID, request, USERNAME));
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
