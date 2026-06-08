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
@DisplayName("CommentService")
class CommentServiceTest {

    private static final Long COMMENT_ID = 100L;
    private static final Long MISSING_COMMENT_ID = 999L;
    private static final Long POST_ID = 10L;
    private static final Long MISSING_POST_ID = 99L;
    private static final Long AUTHOR_ID = 1L;
    private static final Long MISSING_AUTHOR_ID = 2L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    private Post post;
    private User author;
    private Comment comment;
    private CommentRequest request;
    private CommentResponse response;

    @BeforeEach
    void setUp() {
        author = User.builder().id(AUTHOR_ID).username("alice").build();
        post = Post.builder()
                .id(POST_ID)
                .title("Post")
                .body("...")
                .author(author)
                .build();
        comment = Comment.builder()
                .id(COMMENT_ID)
                .body("Nice post!")
                .post(post)
                .author(author)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        request = new CommentRequest("Nice post!", POST_ID, AUTHOR_ID);
        response = new CommentResponse(
                COMMENT_ID,
                "Nice post!",
                POST_ID,
                AUTHOR_ID,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Nested
    @DisplayName("findAllCommentsByPostId")
    class FindAllCommentsByPostId {

        @Test
        @DisplayName("delegates to repository and maps each comment via the mapper")
        void delegatesToRepositoryAndMaps() {
            var pageable = PageRequest.of(0, 20);
            var other = Comment.builder()
                    .id(101L)
                    .body("Great!")
                    .post(post)
                    .author(author)
                    .createdAt(Instant.parse("2024-01-01T01:00:00Z"))
                    .updatedAt(Instant.parse("2024-01-01T01:00:00Z"))
                    .build();
            var otherResponse = new CommentResponse(
                    101L,
                    "Great!",
                    POST_ID,
                    AUTHOR_ID,
                    Instant.parse("2024-01-01T01:00:00Z"),
                    Instant.parse("2024-01-01T01:00:00Z"));
            var page = new PageImpl<>(List.of(comment, other), pageable, 2);
            when(commentRepository.findByPostId(POST_ID, pageable)).thenReturn(page);
            when(commentMapper.toResponse(comment)).thenReturn(response);
            when(commentMapper.toResponse(other)).thenReturn(otherResponse);

            Page<CommentResponse> result = commentService.findAllCommentsByPostId(POST_ID, pageable);

            assertThat(result.getContent()).containsExactly(response, otherResponse);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(commentRepository).findByPostId(POST_ID, pageable);
        }

        @Test
        @DisplayName("returns an empty page when the post has no comments")
        void returnsEmptyPage() {
            var pageable = PageRequest.of(0, 20);
            var empty = new PageImpl<>(List.<Comment>of(), pageable, 0);
            when(commentRepository.findByPostId(POST_ID, pageable)).thenReturn(empty);

            Page<CommentResponse> result = commentService.findAllCommentsByPostId(POST_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(commentMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("findCommentById")
    class FindCommentById {

        @Test
        @DisplayName("returns a mapped response when the comment exists")
        void returnsMappedResponse() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
            when(commentMapper.toResponse(comment)).thenReturn(response);

            CommentResponse result = commentService.findCommentById(COMMENT_ID);

            assertThat(result).isEqualTo(response);
            verify(commentRepository).findById(COMMENT_ID);
            verify(commentMapper).toResponse(comment);
        }

        @Test
        @DisplayName("throws CommentNotFoundException with the missing id when the comment does not exist")
        void throwsWhenMissing() {
            when(commentRepository.findById(MISSING_COMMENT_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    CommentNotFoundException.class, () -> commentService.findCommentById(MISSING_COMMENT_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_COMMENT_ID));

            verify(commentMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("resolves the post and author, persists, and returns the mapped response")
        void createsAndReturns() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));
            when(commentMapper.toEntity(request, post, author)).thenReturn(comment);
            when(commentRepository.save(comment)).thenReturn(comment);
            when(commentMapper.toResponse(comment)).thenReturn(response);

            CommentResponse result = commentService.createComment(request);

            assertThat(result).isEqualTo(response);
            verify(postRepository).findById(POST_ID);
            verify(userRepository).findById(AUTHOR_ID);
            verify(commentMapper).toEntity(request, post, author);
            verify(commentRepository).save(comment);
        }

        @Test
        @DisplayName("throws PostNotFoundException when the post does not exist")
        void throwsWhenPostMissing() {
            when(postRepository.findById(MISSING_POST_ID)).thenReturn(Optional.empty());
            var req = new CommentRequest("Body", MISSING_POST_ID, AUTHOR_ID);

            var thrown = assertThrows(PostNotFoundException.class, () -> commentService.createComment(req));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(userRepository, never()).findById(any());
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when the author does not exist")
        void throwsWhenAuthorMissing() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(userRepository.findById(MISSING_AUTHOR_ID)).thenReturn(Optional.empty());
            var req = new CommentRequest("Body", POST_ID, MISSING_AUTHOR_ID);

            var thrown = assertThrows(UserNotFoundException.class, () -> commentService.createComment(req));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_AUTHOR_ID));

            verify(commentRepository, never()).save(any());
        }
    }
}
