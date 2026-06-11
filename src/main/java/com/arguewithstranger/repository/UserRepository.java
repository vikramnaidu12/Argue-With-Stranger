package com.arguewithstranger.repository;

import com.arguewithstranger.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for the users table.
 *
 * Spring Data JPA derives the SQL for all methods below from
 * their names at application startup — no manual SQL needed.
 *
 * findByUsername  → SELECT * FROM users WHERE username = ?
 * existsByUsername → SELECT COUNT(*) > 0 FROM users WHERE username = ?
 * existsByEmail    → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used by UserDetailsServiceImpl to load the authenticated
     * principal. Spring Security calls this during every
     * JWT-authenticated request.
     *
     * @param username the login username
     * @return Optional.empty() if not found — never throws
     */
    Optional<User> findByUsername(String username);

    /**
     * Used during registration to prevent duplicate usernames.
     * Cheaper than findByUsername because it returns a boolean
     * rather than hydrating the full User object.
     *
     * @param username the desired username
     * @return true if already taken
     */
    boolean existsByUsername(String username);

    /**
     * Used during registration to prevent duplicate emails.
     *
     * @param email the desired email
     * @return true if already registered
     */
    boolean existsByEmail(String email);
}