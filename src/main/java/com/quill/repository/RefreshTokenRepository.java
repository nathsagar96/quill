package com.quill.repository;

import com.quill.model.RefreshToken;
import com.quill.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(UUID token);

    void deleteByUser(User user);
}
