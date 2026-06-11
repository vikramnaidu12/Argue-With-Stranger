package com.arguewithstranger.util;

/**
 * Represents the application-level role of a registered user.
 *
 * USER  — Standard registered user. Can join debates, chat,
 *         spectate, and vote.
 *
 * ADMIN — Elevated privileges. Can create debate topics,
 *         close any debate, and access admin-only endpoints.
 *         In Spring Security this maps to ROLE_ADMIN authority.
 *
 * Spring Security convention: roles are prefixed with "ROLE_"
 * internally. When we call hasRole("ADMIN") in SecurityConfig,
 * Spring automatically checks for "ROLE_ADMIN" in the authorities.
 * Our getAuthority() method handles this mapping explicitly.
 *
 * Stored as EnumType.STRING in MySQL for the same reason as
 * DebateStatus — resilient to reordering.
 */
public enum UserRole {

    USER,
    ADMIN;

    /**
     * Returns the Spring Security authority string for this role.
     * Used when building the GrantedAuthority list in UserDetailsServiceImpl.
     *
     * @return "ROLE_USER" or "ROLE_ADMIN"
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}