package com.quill.mapper;

import com.quill.dto.request.CommentRequest;
import com.quill.dto.response.AuthorResponse;
import com.quill.dto.response.CommentResponse;
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
                toAuthorResponse(entity.getAuthor()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public AuthorResponse toAuthorResponse(User user) {
        return new AuthorResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getBio(), user.getAvatarUrl());
    }

    public Comment toEntity(CommentRequest request, Post post, User author) {
        return Comment.builder().body(request.body()).post(post).author(author).build();
    }
}
