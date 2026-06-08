package com.quill.mapper;

import com.quill.dto.CommentRequest;
import com.quill.dto.CommentResponse;
import com.quill.model.Comment;
import com.quill.model.Post;
import com.quill.model.User;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment entity) {
        return new CommentResponse(
                entity.getId(),
                entity.getBody(),
                entity.getPost().getId(),
                entity.getAuthor().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public Comment toEntity(CommentRequest request, Post post, User author) {
        return Comment.builder().body(request.body()).post(post).author(author).build();
    }
}
