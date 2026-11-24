package com.airline.service;

import com.airline.config.AppProperties;
import com.airline.domain.RefreshSession;
import com.airline.domain.UserAccount;
import com.airline.dto.AuthRequests.LoginRequest;
import com.airline.dto.AuthRequests.PasswordChangeRequest;
import com.airline.dto.LoginResponse;
import com.airline.dto.UserResponse;
import com.airline.error.BadRequestException;
import com.airline.error.UnauthorizedException;
import com.airline.repository.RefreshSessionRepository;
import com.airline.repository.UserAccountRepository;
import com.airline.security.CurrentUser;
import com.airline.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final String BAD_LOGIN = "Неверный email или пароль.";

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final AppProperties properties;
    private final RefreshTokenFactory refreshTokens;

    public AuthService(UserAccountRepository users, RefreshSessionRepository sessions, PasswordEncoder encoder,
                       JwtService jwt, AuditService audit, AppProperties properties, RefreshTokenFactory refreshTokens) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.properties = properties;
        this.refreshTokens = refreshTokens;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount account = users.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (account == null) {
            throw new UnauthorizedException(BAD_LOGIN);
        }
        if (!account.isActive()) {
            throw new UnauthorizedException("Учетная запись отключена.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
            throw new UnauthorizedException("Слишком много неудачных попыток. Вход заблокирован на "
                    + properties.getSecurity().getLockMinutes() + " минут.");
        }
        if (!encoder.matches(request.password(), account.getPasswordHash())) {
            registerFailure(account, now);
            throw new UnauthorizedException(BAD_LOGIN);
        }
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        users.save(account);
        String refresh = refreshTokens.issue(account.getId());
        audit.record(account.getId(), "LOGIN", "UserAccount", account.getId(), "Вход в систему");
        CurrentUser current = new CurrentUser(account);
        return new LoginResponse(
                jwt.accessToken(current),
                refresh,
                properties.getJwt().getAccessMinutes() * 60L,
                account.isMustChangePassword(),
                UserResponse.from(account)
        );
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        RefreshSession session = activeSession(refreshToken);
        UserAccount account = session.getUser();
        if (!account.isActive()) {
            throw new UnauthorizedException("Учетная запись отключена.");
        }
        session.setRevoked(true);
        String refresh = refreshTokens.issue(account.getId());
        CurrentUser current = new CurrentUser(account);
        return new LoginResponse(
                jwt.accessToken(current),
                refresh,
                properties.getJwt().getAccessMinutes() * 60L,
                account.isMustChangePassword(),
                UserResponse.from(account)
        );
    }

    @Transactional
    public void logout(String refreshToken, UUID userId) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessions.findByTokenHash(JwtService.sha256(refreshToken)).ifPresent(session -> session.setRevoked(true));
        }
        audit.record(userId, "LOGOUT", "UserAccount", userId, "Выход из системы");
    }

    @Transactional(readOnly = true)
    public UserResponse currentProfile(UUID userId) {
        UserAccount account = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Требуется вход."));
        return UserResponse.from(account);
    }

    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        UserAccount account = users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Требуется вход."));
        if (!encoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BadRequestException("Текущий пароль указан неверно.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("Новый пароль должен отличаться от текущего.");
        }
        PasswordPolicy.check(request.newPassword());
        account.setPasswordHash(encoder.encode(request.newPassword()));
        account.setMustChangePassword(false);
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        sessions.revokeActiveByUserId(userId);
        audit.record(userId, "PASSWORD_CHANGED", "UserAccount", userId, "Пароль изменен");
    }

    private void registerFailure(UserAccount account, LocalDateTime now) {
        int attempts = account.getFailedAttempts() + 1;
        if (attempts >= properties.getSecurity().getMaxFailedLogins()) {
            account.setFailedAttempts(0);
            account.setLockedUntil(now.plusMinutes(properties.getSecurity().getLockMinutes()));
            users.save(account);
            audit.record(account.getId(), "LOGIN_LOCKED", "UserAccount", account.getId(),
                    "Вход заблокирован после нескольких ошибок");
            return;
        }
        account.setFailedAttempts(attempts);
        users.save(account);
        audit.record(account.getId(), "LOGIN_FAILED", "UserAccount", account.getId(), "Неудачная попытка входа");
    }

    private RefreshSession activeSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Нет refresh-токена. Войдите снова.");
        }
        RefreshSession session = sessions.findByTokenHash(JwtService.sha256(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Сессия не найдена. Войдите снова."));
        if (session.isRevoked() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Сессия истекла. Войдите снова.");
        }
        return session;
    }
}
