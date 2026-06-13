package com.quill.mapper;

import com.quill.dto.response.AuthorResponse;
import com.quill.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public AuthorResponse toAuthorResponse(User user) {
        return new AuthorResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getBio(), user.getAvatarUrl());
    }
}
