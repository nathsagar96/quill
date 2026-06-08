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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService")
class PostServiceTest {

    private static final Long POST_ID = 10L;
    private static final Long MISSING_POST_ID = 99L;
    private static final Long AUTHOR_ID = 1L;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostMapper postMapper;
    @InjectMocks
    private PostService postService;
    private User author;
    private Post post;
    private PostRequest request;
    private PostResponse response;

    @BeforeEach
    void setUp() {
        author = User.builder().id(AUTHOR_ID).username("alice").build();
        post = Post.builder()
                .id(POST_ID)
                .title("Title")
                .body("Body")
                .author(author)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        request = new PostRequest("New title", "New body", AUTHOR_ID);
        response = new PostResponse(POST_ID, "New title", "New body", AUTHOR_ID,
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
            var other = Post.builder().id(11L).title("Other").body("...").author(author).build();
            var otherResponse = new PostResponse(11L, "Other", "...", AUTHOR_ID, null, null);
            var page = new PageImpl<>(List.of(post, other), pageable, 2);
            when(postRepository.findAll(pageable)).thenReturn(page);
            when(postMapper.toResponse(post)).thenReturn(response);
            when(postMapper.toResponse(other)).thenReturn(otherResponse);

            Page<PostResponse> result = postService.findAllPosts(pageable);

            assertThat(result.getContent()).containsExactly(response, otherResponse);
            assertThat(result.getTotalElements()).isEqualTo(2);
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

            var thrown = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.findPostById(MISSING_POST_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(postMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("resolves the author, persists, and returns the mapped response")
        void createsAndReturns() {
            when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
            when(postMapper.toEntity(request, author)).thenReturn(post);
            when(postRepository.save(post)).thenReturn(post);
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.createPost(request);

            assertThat(result).isEqualTo(response);
            verify(userRepository).findById(AUTHOR_ID);
            verify(postMapper).toEntity(request, author);
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("throws UserNotFoundException with the missing id when the author does not exist")
        void throwsWhenAuthorMissing() {
            when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    UserNotFoundException.class,
                    () -> postService.createPost(request));
            assertThat(thrown).hasMessageContaining(String.valueOf(AUTHOR_ID));

            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("mutates the loaded entity and returns the mapped response")
        void updatesAndReturns() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
            when(postMapper.toResponse(post)).thenReturn(response);

            PostResponse result = postService.updatePost(POST_ID, request);

            assertThat(result).isEqualTo(response);
            assertThat(post.getTitle()).isEqualTo(request.title());
            assertThat(post.getBody()).isEqualTo(request.body());
            assertThat(post.getAuthor()).isEqualTo(author);
        }

        @Test
        @DisplayName("throws PostNotFoundException with the missing id when the post does not exist")
        void throwsWhenPostMissing() {
            when(postRepository.findById(MISSING_POST_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.updatePost(MISSING_POST_ID, request));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(userRepository, never()).findById(AUTHOR_ID);
        }

        @Test
        @DisplayName("throws UserNotFoundException with the missing id when the new author does not exist")
        void throwsWhenAuthorMissing() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    UserNotFoundException.class,
                    () -> postService.updatePost(POST_ID, request));
            assertThat(thrown).hasMessageContaining(String.valueOf(AUTHOR_ID));
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

            var thrown = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.deletePost(MISSING_POST_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(postRepository, never()).deleteById(MISSING_POST_ID);
        }
    }
}
