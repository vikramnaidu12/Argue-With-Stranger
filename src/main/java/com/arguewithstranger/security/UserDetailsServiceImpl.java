package com.arguewithstranger.security;

import com.arguewithstranger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security's user loading contract implementation.
 *
 * Called in two contexts:
 *
 *   1. During LOGIN — Spring Security's AuthenticationManager
 *      calls loadUserByUsername() to get the stored UserDetails,
 *      then compares the provided password against the BCrypt hash.
 *
 *   2. During JWT FILTER — JwtAuthFilter calls loadUserByUsername()
 *      after extracting the username from a valid JWT, to build
 *      the Authentication object for the SecurityContext.
 *
 * @Transactional(readOnly = true) — ensures this runs within a
 * transaction so Hibernate can lazily load any collections if
 * needed, and marks the transaction as read-only for a slight
 * performance benefit (no dirty checking, no flush).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by username for Spring Security authentication.
     *
     * Our User entity implements UserDetails directly, so we
     * return it as-is. Spring Security will use:
     *   - getPassword()     → the BCrypt hash for comparison
     *   - getAuthorities()  → [ROLE_USER] or [ROLE_ADMIN]
     *   - isEnabled()       → true (we don't lock accounts here)
     *
     * @param username the username to look up
     * @return the User entity as a UserDetails instance
     * @throws UsernameNotFoundException if no user found — Spring
     *         Security catches this and throws BadCredentialsException
     *         to avoid revealing whether the username exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.debug("Loading user by username: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException(
                            "User not found with username: " + username);
                });
    }
}