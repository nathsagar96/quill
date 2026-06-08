package com.quill.service;

import com.quill.dto.request.UpdateProfileRequest;
import com.quill.dto.response.AuthorResponse;
import com.quill.exception.UserNotFoundException;
import com.quill.model.User;
import com.quill.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public AuthorResponse getProfile(String username) {
        User user = findUserByUsername(username);
        return toAuthorResponse(user);
    }

    @Transactional
    public AuthorResponse updateProfile(String username, UpdateProfileRequest request) {
        log.info("Updating profile for user '{}'", username);
        User user = findUserByUsername(username);
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        User saved = userRepository.save(user);
        log.info("Updated profile for user id={}", saved.getId());
        return toAuthorResponse(saved);
    }

    private User findUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    private AuthorResponse toAuthorResponse(User user) {
        return new AuthorResponse(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getBio(), user.getAvatarUrl());
    }
}
