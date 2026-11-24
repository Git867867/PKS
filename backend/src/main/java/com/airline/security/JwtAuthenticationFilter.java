package com.airline.security;

import com.airline.domain.UserAccount;
import com.airline.error.ErrorWriter;
import com.airline.repository.UserAccountRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String subject = jwtService.parse(header.substring(7)).getSubject();
            UserAccount account = users.findById(UUID.fromString(subject)).orElse(null);
            if (account == null || !account.isActive()) {
                ErrorWriter.write(request, response, 401, "Сессия недействительна. Войдите снова.", null);
                return;
            }
            if (account.isMustChangePassword() && !passwordChangeAllowed(request)) {
                ErrorWriter.write(request, response, 403,
                        "Сначала смените временный пароль.", "PASSWORD_CHANGE_REQUIRED");
                return;
            }
            CurrentUser principal = new CurrentUser(account);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            ErrorWriter.write(request, response, 401, "Сессия истекла. Войдите снова.", null);
        }
    }

    private boolean passwordChangeAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (HttpMethod.POST.matches(request.getMethod()) && path.equals("/api/auth/login")) {
            return true;
        }
        if (HttpMethod.POST.matches(request.getMethod()) && path.equals("/api/auth/refresh")) {
            return true;
        }
        if (HttpMethod.POST.matches(request.getMethod()) && path.equals("/api/auth/password")) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && path.equals("/api/auth/me")) {
            return true;
        }
        return HttpMethod.POST.matches(request.getMethod()) && path.equals("/api/auth/logout");
    }
}
