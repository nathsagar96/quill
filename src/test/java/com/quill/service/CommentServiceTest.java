package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quill.dto.request.CommentRequest;
import com.quill.dto.response.AuthorResponse;
import com.quill.dto.response.CommentResponse;
import com.quill.exception.CommentNotFoundException;
import com.quill.exception.ForbiddenOperationException;
import com.quill.exception.PostNotFoundException;
import com.quill.exception.UserNotFoundException;
import com.quill.mapper.CommentMapper;
import com.quill.model.Comment;
import com.quill.model.Post;
import com.quill.model.User;
import com.quill.repository.CommentRepository;
import com.quill.repository.PostRepository;
import com.quill.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
@DisplayName("CommentService")
class CommentServiceTest {

    private static final Long COMMENT_ID = 100L;
    private static final Long POST_ID = 10L;
    private static final Long MISSING_POST_ID = 99L;
    private static final Long AUTHOR_ID = 1L;
    private static final String USERNAME = "alice";

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
        request = new CommentRequest("Nice post!");
        response = new CommentResponse(
                COMMENT_ID,
                "Nice post!",
                POST_ID,
                new AuthorResponse(AUTHOR_ID, null, null, null, null),
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
                    new AuthorResponse(AUTHOR_ID, null, null, null, null),
                    Instant.parse("2024-01-01T01:00:00Z"),
                    Instant.parse("2024-01-01T01:00:00Z"));
            var page = new PageImpl<>(List.of(comment, other), pageable, 2);
            when(postRepository.existsById(POST_ID)).thenReturn(true);
            when(commentRepository.findByPostId(POST_ID, pageable)).thenReturn(page);
            when(commentMapper.toResponse(comment)).thenReturn(response);
            when(commentMapper.toResponse(other)).thenReturn(otherResponse);

            Page<CommentResponse> result = commentService.findAllCommentsByPostId(POST_ID, pageable);

            assertThat(result.getContent()).containsExactly(response, otherResponse);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(postRepository).existsById(POST_ID);
            verify(commentRepository).findByPostId(POST_ID, pageable);
        }

        @Test
        @DisplayName("returns an empty page when the post has no comments")
        void returnsEmptyPage() {
            var pageable = PageRequest.of(0, 20);
            var empty = new PageImpl<>(List.<Comment>of(), pageable, 0);
            when(postRepository.existsById(POST_ID)).thenReturn(true);
            when(commentRepository.findByPostId(POST_ID, pageable)).thenReturn(empty);

            Page<CommentResponse> result = commentService.findAllCommentsByPostId(POST_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(postRepository).existsById(POST_ID);
            verify(commentMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("throws PostNotFoundException when the post does not exist")
        void throwsWhenPostMissing() {
            var pageable = PageRequest.of(0, 20);
            when(postRepository.existsById(MISSING_POST_ID)).thenReturn(false);

            var thrown = assertThrows(
                    PostNotFoundException.class,
                    () -> commentService.findAllCommentsByPostId(MISSING_POST_ID, pageable));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(commentRepository, never()).findByPostId(any(), any());
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("resolves the post and author by username, persists, and returns the mapped response")
        void createsAndReturns() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(author));
            when(commentMapper.toEntity(request, post, author)).thenReturn(comment);
            when(commentRepository.save(comment)).thenReturn(comment);
            when(commentMapper.toResponse(comment)).thenReturn(response);

            CommentResponse result = commentService.createComment(request, POST_ID, USERNAME);

            assertThat(result).isEqualTo(response);
            verify(postRepository).findById(POST_ID);
            verify(userRepository).findByUsername(USERNAME);
            verify(commentMapper).toEntity(request, post, author);
            verify(commentRepository).save(comment);
        }

        @Test
        @DisplayName("throws PostNotFoundException when the post does not exist")
        void throwsWhenPostMissing() {
            when(postRepository.findById(MISSING_POST_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    PostNotFoundException.class,
                    () -> commentService.createComment(request, MISSING_POST_ID, USERNAME));
            assertThat(thrown).hasMessageContaining(String.valueOf(MISSING_POST_ID));

            verify(userRepository, never()).findByUsername(any());
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when the author does not exist")
        void throwsWhenAuthorMissing() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    UserNotFoundException.class, () -> commentService.createComment(request, POST_ID, USERNAME));
            assertThat(thrown).hasMessageContaining(USERNAME);

            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        private static final Long OTHER_USER_ID = 2L;
        private static final String OTHER_USERNAME = "bob";

        @Test
        @DisplayName("updates the body and returns the mapped response")
        void updatesAndReturns() {
            var updateRequest = new CommentRequest("Updated body");
            var updatedResponse = new CommentResponse(
                    COMMENT_ID,
                    "Updated body",
                    POST_ID,
                    new AuthorResponse(AUTHOR_ID, null, null, null, null),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt());
            when(commentRepository.findByIdAndPostId(COMMENT_ID, POST_ID)).thenReturn(Optional.of(comment));
            when(commentMapper.toResponse(comment)).thenReturn(updatedResponse);

            CommentResponse result = commentService.updateComment(POST_ID, COMMENT_ID, updateRequest, USERNAME);

            assertThat(result).isEqualTo(updatedResponse);
            assertThat(comment.getBody()).isEqualTo("Updated body");
            verify(commentRepository).findByIdAndPostId(COMMENT_ID, POST_ID);
            verify(commentMapper).toResponse(comment);
        }

        @Test
        @DisplayName("throws CommentNotFoundException when the comment does not exist")
        void throwsWhenCommentMissing() {
            when(commentRepository.findByIdAndPostId(COMMENT_ID, POST_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    CommentNotFoundException.class,
                    () -> commentService.updateComment(POST_ID, COMMENT_ID, request, USERNAME));
            assertThat(thrown).hasMessageContaining(String.valueOf(COMMENT_ID));
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when the user is not the owner")
        void throwsWhenNotOwner() {
            when(commentRepository.findByIdAndPostId(COMMENT_ID, POST_ID)).thenReturn(Optional.of(comment));

            var thrown = assertThrows(
                    ForbiddenOperationException.class,
                    () -> commentService.updateComment(POST_ID, COMMENT_ID, request, OTHER_USERNAME));
            assertThat(thrown).hasMessageContaining(OTHER_USERNAME).hasMessageContaining(String.valueOf(COMMENT_ID));
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("deletes the comment when it exists")
        void deletesWhenExists() {
            when(commentRepository.findByIdAndPostId(COMMENT_ID, POST_ID)).thenReturn(Optional.of(comment));

            commentService.deleteComment(POST_ID, COMMENT_ID);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("throws CommentNotFoundException when the comment does not exist")
        void throwsWhenCommentMissing() {
            when(commentRepository.findByIdAndPostId(COMMENT_ID, POST_ID)).thenReturn(Optional.empty());

            var thrown = assertThrows(
                    CommentNotFoundException.class, () -> commentService.deleteComment(POST_ID, COMMENT_ID));
            assertThat(thrown).hasMessageContaining(String.valueOf(COMMENT_ID));
        }
    }
}
