package com.quill.scheduler;

import com.quill.repository.PasswordResetTokenRepository;
import com.quill.repository.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        passwordResetTokenRepository.deleteByExpiresAtBeforeOrUsedTrue(Instant.now());
        log.info("Cleaned up expired/used password reset tokens");
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedTrue(Instant.now());
        log.info("Cleaned up expired/revoked refresh tokens");
    }
}
