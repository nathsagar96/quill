package com.quill.mapper;

import com.quill.dto.TagRequest;
import com.quill.dto.TagResponse;
import com.quill.model.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag entity) {
        return new TagResponse(entity.getId(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public Tag toEntity(TagRequest request) {
        return Tag.builder().name(request.name()).build();
    }
}
