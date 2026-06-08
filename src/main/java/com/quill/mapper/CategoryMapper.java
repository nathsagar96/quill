package com.quill.mapper;

import com.quill.dto.request.CategoryRequest;
import com.quill.dto.response.CategoryResponse;
import com.quill.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category entity) {
        return new CategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .build();
    }
}
