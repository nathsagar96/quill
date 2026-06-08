package com.quill.mapper;

import com.quill.dto.PostRequest;
import com.quill.dto.PostResponse;
import com.quill.model.Category;
import com.quill.model.Post;
import com.quill.model.Tag;
import com.quill.model.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post entity) {
        return new PostResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getBody(),
                entity.getExcerpt(),
                entity.getAuthor().getId(),
                entity.getCategories().stream().map(Category::getId).collect(Collectors.toSet()),
                entity.getTags().stream().map(Tag::getId).collect(Collectors.toSet()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public Post toEntity(PostRequest request, User author, Set<Category> categories, Set<Tag> tags) {
        return Post.builder()
                .title(request.title())
                .body(request.body())
                .excerpt(request.excerpt())
                .author(author)
                .categories(categories)
                .tags(tags)
                .build();
    }
}
