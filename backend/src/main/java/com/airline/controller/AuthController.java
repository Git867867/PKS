package com.airline.controller;

import com.airline.config.AppProperties;
import com.airline.dto.AuthRequests.LoginRequest;
import com.airline.dto.AuthRequests.PasswordChangeRequest;
import com.airline.dto.AuthRequests.RefreshRequest;
import com.airline.dto.LoginResponse;
import com.airline.dto.UserResponse;
import com.airline.security.Auth;
import com.airline.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AppProperties properties;

    public AuthController(AuthService auth, AppProperties properties) {
        this.auth = auth;
        this.properties = properties;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse result = auth.login(request);
        cookie(response, result.refreshToken(), properties.getJwt().getRefreshDays() * 24L * 60L * 60L);
        return result;
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody(required = false) RefreshRequest body,
                                 @CookieValue(name = "refreshToken", required = false) String cookie,
                                 HttpServletResponse response) {
        String token = body != null && body.refreshToken() != null && !body.refreshToken().isBlank()
                ? body.refreshToken() : cookie;
        LoginResponse result = auth.refresh(token);
        cookie(response, result.refreshToken(), properties.getJwt().getRefreshDays() * 24L * 60L * 60L);
        return result;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest body,
                                       @CookieValue(name = "refreshToken", required = false) String cookie,
                                       HttpServletResponse response) {
        String token = body != null && body.refreshToken() != null && !body.refreshToken().isBlank()
                ? body.refreshToken() : cookie;
        auth.logout(token, Auth.require().getId());
        cookie(response, "", 0);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me() {
        return auth.currentProfile(Auth.require().getId());
    }

    @PostMapping("/password")
    public ResponseEntity<Void> password(@Valid @RequestBody PasswordChangeRequest request, HttpServletResponse response) {
        auth.changePassword(Auth.require().getId(), request);
        cookie(response, "", 0);
        return ResponseEntity.noContent().build();
    }

    private void cookie(HttpServletResponse response, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(properties.getSecurity().isCookieSecure())
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
