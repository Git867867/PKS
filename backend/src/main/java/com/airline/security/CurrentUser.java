package com.airline.security;

import com.airline.domain.RoleName;
import com.airline.domain.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class CurrentUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final Set<RoleName> roles;
    private final boolean active;
    private final boolean mustChangePassword;

    public CurrentUser(UserAccount account) {
        this.id = account.getId();
        this.email = account.getEmail();
        this.fullName = account.getFullName();
        this.roles = Set.copyOf(account.getRoles());
        this.active = account.isActive();
        this.mustChangePassword = account.isMustChangePassword();
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public Set<RoleName> getRoles() { return roles; }
    public boolean isMustChangePassword() { return mustChangePassword; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
