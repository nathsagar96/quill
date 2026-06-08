package com.quill.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quill.TestcontainersConfiguration;
import com.quill.config.JpaConfig;
import com.quill.model.Comment;
import com.quill.model.Post;
import com.quill.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@DisplayName("CommentRepository")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = userRepository.saveAndFlush(User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hashed-pw")
                .build());
        post = postRepository.saveAndFlush(
                Post.builder().title("A post").body("Post body").author(author).build());
    }

    @Test
    @DisplayName("should persist a comment with a post and author and assign an id")
    void shouldPersistComment() {
        Comment comment =
                Comment.builder().body("Great post!").post(post).author(author).build();

        Comment saved = commentRepository.saveAndFlush(comment);

        assertThat(saved.getId()).isNotNull();
        assertThat(commentRepository.findById(saved.getId())).isPresent().get().satisfies(found -> {
            assertThat(found.getBody()).isEqualTo("Great post!");
            assertThat(found.getPost().getId()).isEqualTo(post.getId());
            assertThat(found.getAuthor().getId()).isEqualTo(author.getId());
        });
    }

    @Test
    @DisplayName("should return comments for a post ordered by id with pagination metadata")
    void shouldFindCommentsByPostIdWithPaging() {
        for (int i = 1; i <= 4; i++) {
            commentRepository.saveAndFlush(Comment.builder()
                    .body("Comment %d".formatted(i))
                    .post(post)
                    .author(author)
                    .build());
        }

        Pageable first = PageRequest.of(0, 2, Sort.by("id"));
        Pageable second = PageRequest.of(1, 2, Sort.by("id"));

        Page<Comment> page0 = commentRepository.findByPostId(post.getId(), first);
        Page<Comment> page1 = commentRepository.findByPostId(post.getId(), second);

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(4);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page0.getContent().getFirst().getId())
                .isLessThan(page1.getContent().getFirst().getId());
    }

    @Test
    @DisplayName("should return empty page when the post has no comments")
    void shouldReturnEmptyPageWhenNoComments() {
        Page<Comment> page = commentRepository.findByPostId(post.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("should return empty page when the post id does not exist")
    void shouldReturnEmptyPageForNonExistentPost() {
        Page<Comment> page = commentRepository.findByPostId(99_999L, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("should return empty Optional when comment id does not exist")
    void shouldReturnEmptyWhenCommentNotFound() {
        assertThat(commentRepository.findById(99_999L)).isEmpty();
    }

    @Test
    @DisplayName("should reject a comment with a null body (NOT NULL column)")
    void shouldRejectCommentWithNullBody() {
        Comment comment = Comment.builder().body(null).post(post).author(author).build();

        assertThatThrownBy(() -> commentRepository.saveAndFlush(comment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should reject a comment with a null post (NOT NULL FK)")
    void shouldRejectCommentWithNullPost() {
        Comment comment =
                Comment.builder().body("Body").post(null).author(author).build();

        assertThatThrownBy(() -> commentRepository.saveAndFlush(comment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should reject a comment with a null author (NOT NULL FK)")
    void shouldRejectCommentWithNullAuthor() {
        Comment comment = Comment.builder().body("Body").post(post).author(null).build();

        assertThatThrownBy(() -> commentRepository.saveAndFlush(comment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
