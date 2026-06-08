package com.quill.mapper;

import com.quill.dto.PostRequest;
import com.quill.dto.PostResponse;
import com.quill.model.Post;
import com.quill.model.User;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post entity) {
        return new PostResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getBody(),
                entity.getAuthor().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public Post toEntity(PostRequest request, User author) {
        return Post.builder()
                .title(request.title())
                .body(request.body())
                .author(author)
                .build();
    }
}
