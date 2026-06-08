package com.quill.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.quill.dto.CommentRequest;
import com.quill.dto.CommentResponse;
import com.quill.model.Comment;
import com.quill.model.Post;
import com.quill.model.User;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CommentMapper")
class CommentMapperTest {

    private final CommentMapper mapper = new CommentMapper();

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("maps every field including the post's and author's ids")
        void mapsAllFields() {
            var created = Instant.parse("2024-01-01T00:00:00Z");
            var updated = Instant.parse("2024-02-02T00:00:00Z");
            var post = Post.builder().id(10L).build();
            var author = User.builder().id(7L).build();
            var comment = Comment.builder()
                    .id(42L)
                    .body("Nice post!")
                    .post(post)
                    .author(author)
                    .createdAt(created)
                    .updatedAt(updated)
                    .build();

            CommentResponse response = mapper.toResponse(comment);

            assertThat(response.id()).isEqualTo(42L);
            assertThat(response.body()).isEqualTo("Nice post!");
            assertThat(response.postId()).isEqualTo(10L);
            assertThat(response.authorId()).isEqualTo(7L);
            assertThat(response.createdAt()).isEqualTo(created);
            assertThat(response.updatedAt()).isEqualTo(updated);
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("sets body, post, and author from the request and supplied entities")
        void setsBodyPostAndAuthor() {
            var request = new CommentRequest("Nice post!");
            var post = Post.builder().id(10L).build();
            var author = User.builder().id(7L).build();

            Comment entity = mapper.toEntity(request, post, author);

            assertThat(entity.getBody()).isEqualTo("Nice post!");
            assertThat(entity.getPost()).isSameAs(post);
            assertThat(entity.getAuthor()).isSameAs(author);
        }

        @Test
        @DisplayName("leaves id, audit fields, and post/author collections to JPA")
        void doesNotSetPersistedFields() {
            var request = new CommentRequest("Nice post!");
            var post = Post.builder().id(10L).build();
            var author = User.builder().id(7L).build();

            Comment entity = mapper.toEntity(request, post, author);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
        }
    }
}
