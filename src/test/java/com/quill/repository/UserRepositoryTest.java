package com.quill.repository;

import com.quill.TestcontainersConfiguration;
import com.quill.config.JpaConfig;
import com.quill.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should persist a user and assign an id")
    void shouldPersistUser() {
        User user = User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hashed-pw")
                .build();

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent().get().satisfies(found -> {
            assertThat(found.getUsername()).isEqualTo("bob");
            assertThat(found.getEmail()).isEqualTo("bob@example.com");
        });
    }

    @Test
    @DisplayName("should find all users")
    void shouldFindAllUsers() {
        userRepository.saveAndFlush(User.builder()
                .username("carol")
                .email("carol@example.com")
                .passwordHash("h")
                .build());
        userRepository.saveAndFlush(User.builder()
                .username("dave")
                .email("dave@example.com")
                .passwordHash("h")
                .build());

        List<User> users = userRepository.findAll();

        assertThat(users).extracting(User::getUsername).contains("carol", "dave");
    }

    @Test
    @DisplayName("should reject a user with a null username (NOT NULL column)")
    void shouldRejectUserWithNullUsername() {
        User user = User.builder()
                .username(null)
                .email("nope@example.com")
                .passwordHash("h")
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should reject a duplicate username (UNIQUE constraint)")
    void shouldRejectDuplicateUsername() {
        userRepository.saveAndFlush(User.builder()
                .username("dupe")
                .email("first@example.com")
                .passwordHash("h")
                .build());

        User duplicate = User.builder()
                .username("dupe")
                .email("second@example.com")
                .passwordHash("h")
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should reject a duplicate email (UNIQUE constraint)")
    void shouldRejectDuplicateEmail() {
        userRepository.saveAndFlush(User.builder()
                .username("u1")
                .email("same@example.com")
                .passwordHash("h")
                .build());

        User duplicate = User.builder()
                .username("u2")
                .email("same@example.com")
                .passwordHash("h")
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
