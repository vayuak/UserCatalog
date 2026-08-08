package com.UserCatalogServiceOne.UserCatalog.Models;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    private final Long id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    // 🟢 ADDED: Safe stateless premium boolean carrier flag
    private final boolean isPremium;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        // 🟢 COMPACTED: Convert char directly to an authority tag string marker without memory overhead
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(String.valueOf('U')));

        // 🟢 EXTRACT PRIVILEGE CLAIM: Map it during object conversion
        // Note: Change to user.isPremium() if your JPA User model doesn't use standard getIsPremium()
        this.isPremium = user.isPremium();
    }

    public Long getId() { return id; }

    // 🟢 EXPOSE PRIVILEGE ACCESSOR: This satisfies the method hook inside your JwtUtils token builder
    public boolean isPremium() { return isPremium; }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}