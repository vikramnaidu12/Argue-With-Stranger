package com.arguewithstranger.entity;

import com.arguewithstranger.util.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Represents a registered user of the platform.
 *
 * Implements UserDetails so this entity can be used directly
 * by Spring Security without a separate wrapper object.
 *
 * The @Table unique constraints enforce at the database level
 * that no two users share the same username or email — even
 * if application-level validation is somehow bypassed.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email",    columnNames = "email")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    // BCrypt hash — never store plain text
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Lifecycle ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── UserDetails contract ───────────────────────────────────

    /**
     * Maps our UserRole enum to a Spring Security GrantedAuthority.
     * Returns e.g. [SimpleGrantedAuthority("ROLE_USER")]
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    /**
     * Spring Security calls getPassword() to retrieve the stored
     * BCrypt hash for comparison during authentication.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Spring Security calls getUsername() as the principal identifier.
     * We use the username field (not email) as the login identifier.
     */
    @Override
    public String getUsername() {
        return username;
    }

    // All four account status methods return true — we do not
    // implement account locking or expiry in this version.
    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return true; }
}