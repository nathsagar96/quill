package com.quill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.quill.dto.request.UpdateProfileRequest;
import com.quill.dto.response.AuthorResponse;
import com.quill.exception.UserNotFoundException;
import com.quill.mapper.UserMapper;
import com.quill.model.User;
import com.quill.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "alice";
    private static final String DISPLAY_NAME = "Alice";
    private static final String BIO = "A bio";
    private static final String AVATAR_URL = "https://example.com/avatar.jpg";

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .displayName(DISPLAY_NAME)
                .bio(BIO)
                .avatarUrl(AVATAR_URL)
                .build();
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("returns AuthorResponse when user exists")
        void returnsProfile() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            AuthorResponse result = userService.getProfile(USERNAME);

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.username()).isEqualTo(USERNAME);
            assertThat(result.displayName()).isEqualTo(DISPLAY_NAME);
            assertThat(result.bio()).isEqualTo(BIO);
            assertThat(result.avatarUrl()).isEqualTo(AVATAR_URL);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void throwsWhenUserNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getProfile("unknown"));
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("updates all fields when all provided")
        void updatesAllFields() {
            var request = new UpdateProfileRequest("New Name", "New bio", "https://example.com/new.jpg");
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            AuthorResponse result = userService.updateProfile(USERNAME, request);

            assertThat(result.displayName()).isEqualTo("New Name");
            assertThat(result.bio()).isEqualTo("New bio");
            assertThat(result.avatarUrl()).isEqualTo("https://example.com/new.jpg");
        }

        @Test
        @DisplayName("updates only displayName when bio and avatarUrl are null")
        void updatesOnlyDisplayName() {
            var request = new UpdateProfileRequest("New Name", null, null);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            AuthorResponse result = userService.updateProfile(USERNAME, request);

            assertThat(result.displayName()).isEqualTo("New Name");
            assertThat(result.bio()).isEqualTo(BIO);
            assertThat(result.avatarUrl()).isEqualTo(AVATAR_URL);
        }

        @Test
        @DisplayName("updates only bio when displayName and avatarUrl are null")
        void updatesOnlyBio() {
            var request = new UpdateProfileRequest(null, "New bio", null);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            AuthorResponse result = userService.updateProfile(USERNAME, request);

            assertThat(result.displayName()).isEqualTo(DISPLAY_NAME);
            assertThat(result.bio()).isEqualTo("New bio");
            assertThat(result.avatarUrl()).isEqualTo(AVATAR_URL);
        }

        @Test
        @DisplayName("updates only avatarUrl when displayName and bio are null")
        void updatesOnlyAvatarUrl() {
            var request = new UpdateProfileRequest(null, null, "https://example.com/new.jpg");
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

            AuthorResponse result = userService.updateProfile(USERNAME, request);

            assertThat(result.displayName()).isEqualTo(DISPLAY_NAME);
            assertThat(result.bio()).isEqualTo(BIO);
            assertThat(result.avatarUrl()).isEqualTo("https://example.com/new.jpg");
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void throwsWhenUserNotFound() {
            var request = new UpdateProfileRequest("Name", null, null);
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.updateProfile("unknown", request));
        }
    }
}
