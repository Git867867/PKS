package com.airline.service;

import com.airline.config.AppProperties;
import com.airline.domain.RefreshSession;
import com.airline.domain.UserAccount;
import com.airline.error.UnauthorizedException;
import com.airline.repository.RefreshSessionRepository;
import com.airline.repository.UserAccountRepository;
import com.airline.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenFactory {

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenFactory(UserAccountRepository users, RefreshSessionRepository sessions, AppProperties properties) {
        this.users = users;
        this.sessions = sessions;
        this.properties = properties;
    }

    @Transactional
    public String issue(UUID userId) {
        UserAccount account = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден."));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        RefreshSession session = new RefreshSession();
        session.setId(UUID.randomUUID());
        session.setUser(account);
        session.setTokenHash(JwtService.sha256(token));
        session.setExpiresAt(now.plusDays(properties.getJwt().getRefreshDays()));
        session.setRevoked(false);
        session.setCreatedAt(now);
        sessions.save(session);
        return token;
    }
}
