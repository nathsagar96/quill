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
@DisplayName("PostRepository")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;

    @BeforeEach
    void setUp() {
        author = userRepository.saveAndFlush(User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hashed-pw")
                .build());
    }

    @Test
    @DisplayName("should persist a post with an author and assign an id")
    void shouldPersistPostWithAuthor() {
        Post post = Post.builder()
                .title("Hello, World")
                .body("First post body")
                .slug("hello-world")
                .author(author)
                .build();

        Post saved = postRepository.saveAndFlush(post);

        assertThat(saved.getId()).isNotNull();
        assertThat(postRepository.findById(saved.getId())).isPresent().get().satisfies(found -> {
            assertThat(found.getTitle()).isEqualTo("Hello, World");
            assertThat(found.getBody()).isEqualTo("First post body");
            assertThat(found.getAuthor().getId()).isEqualTo(author.getId());
        });
    }

    @Test
    @DisplayName("should return empty Optional when post id does not exist")
    void shouldReturnEmptyWhenPostNotFound() {
        assertThat(postRepository.findById(99_999L)).isEmpty();
    }

    @Test
    @DisplayName("should delete a post by id")
    void shouldDeletePost() {
        Post saved = postRepository.saveAndFlush(Post.builder()
                .title("To delete")
                .body("Body")
                .slug("to-delete")
                .author(author)
                .build());
        Long id = saved.getId();

        postRepository.deleteById(id);
        postRepository.flush();

        assertThat(postRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("should update an existing post")
    void shouldUpdatePost() {
        Post saved = postRepository.saveAndFlush(Post.builder()
                .title("Old title")
                .body("Old body")
                .slug("old-title")
                .author(author)
                .build());
        saved.setTitle("New title");
        saved.setBody("New body");
        postRepository.flush();

        assertThat(postRepository.findById(saved.getId())).get().satisfies(p -> {
            assertThat(p.getTitle()).isEqualTo("New title");
            assertThat(p.getBody()).isEqualTo("New body");
        });
    }

    @Test
    @DisplayName("should return all posts with pagination metadata")
    void shouldFindAllPostsWithPaging() {
        for (int i = 0; i < 5; i++) {
            postRepository.saveAndFlush(Post.builder()
                    .title("Post %d".formatted(i))
                    .body("Body %d".formatted(i))
                    .slug("post-%d".formatted(i))
                    .author(author)
                    .build());
        }

        Pageable first = PageRequest.of(0, 2, Sort.by("id"));
        Pageable second = PageRequest.of(1, 2, Sort.by("id"));

        Page<Post> page0 = postRepository.findAll(first);
        Page<Post> page1 = postRepository.findAll(second);

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page0.getContent().getFirst().getId())
                .isLessThan(page1.getContent().getFirst().getId());
    }

    @Test
    @DisplayName("should cascade-persist a comment added to a new post")
    void shouldCascadePersistComments() {
        Post post = Post.builder()
                .title("With comment")
                .body("Body")
                .slug("with-comment")
                .author(author)
                .build();
        post.getComments()
                .add(Comment.builder()
                        .body("Nice post!")
                        .post(post)
                        .author(author)
                        .build());

        Post saved = postRepository.saveAndFlush(post);

        assertThat(saved.getId()).isNotNull();
        Post reloaded = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getComments()).hasSize(1);
        assertThat(reloaded.getComments().getFirst().getBody()).isEqualTo("Nice post!");
    }

    @Test
    @DisplayName("should reject a post with a null author (NOT NULL FK)")
    void shouldRejectPostWithNullAuthor() {
        Post post =
                Post.builder().title("No author").body("Body").slug("no-author").build();

        assertThatThrownBy(() -> postRepository.saveAndFlush(post)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should reject a post with a null title (NOT NULL column)")
    void shouldRejectPostWithNullTitle() {
        Post post = Post.builder()
                .title(null)
                .body("Body")
                .slug("null-title")
                .author(author)
                .build();

        assertThatThrownBy(() -> postRepository.saveAndFlush(post)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should return empty page when no posts exist")
    void shouldReturnEmptyPageWhenNoPosts() {
        Page<Post> page = postRepository.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("should find a post by slug")
    void shouldFindPostBySlug() {
        postRepository.saveAndFlush(Post.builder()
                .title("My Post")
                .body("Body")
                .slug("my-post")
                .author(author)
                .build());

        assertThat(postRepository.findBySlug("my-post")).isPresent().get().satisfies(p -> {
            assertThat(p.getTitle()).isEqualTo("My Post");
            assertThat(p.getSlug()).isEqualTo("my-post");
        });
    }

    @Test
    @DisplayName("should return empty when slug does not exist")
    void shouldReturnEmptyWhenSlugNotFound() {
        assertThat(postRepository.findBySlug("non-existent")).isEmpty();
    }

    @Test
    @DisplayName("should return true from existsBySlug when slug exists")
    void shouldReturnTrueWhenSlugExists() {
        postRepository.saveAndFlush(Post.builder()
                .title("Exists")
                .body("Body")
                .slug("exists")
                .author(author)
                .build());

        assertThat(postRepository.existsBySlug("exists")).isTrue();
        assertThat(postRepository.existsBySlug("missing")).isFalse();
    }

    @Test
    @DisplayName("should reject a post with a null slug (NOT NULL column)")
    void shouldRejectNullSlug() {
        Post post = Post.builder().title("No slug").body("Body").author(author).build();

        assertThatThrownBy(() -> postRepository.saveAndFlush(post)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should reject a duplicate slug (UNIQUE constraint)")
    void shouldRejectDuplicateSlug() {
        postRepository.saveAndFlush(Post.builder()
                .title("First")
                .body("Body")
                .slug("same-slug")
                .author(author)
                .build());

        Post duplicate = Post.builder()
                .title("Second")
                .body("Body")
                .slug("same-slug")
                .author(author)
                .build();

        assertThatThrownBy(() -> postRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
