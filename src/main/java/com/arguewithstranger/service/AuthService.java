package com.arguewithstranger.service;

import com.arguewithstranger.dto.request.LoginRequest;
import com.arguewithstranger.dto.request.RegisterRequest;
import com.arguewithstranger.dto.response.AuthResponse;
import com.arguewithstranger.entity.User;
import com.arguewithstranger.exception.AuthException;
import com.arguewithstranger.repository.UserRepository;
import com.arguewithstranger.security.JwtUtil;
import com.arguewithstranger.util.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and login.
 *
 * Registration flow:
 *   1. Check username not already taken
 *   2. Check email not already registered
 *   3. Hash password with BCrypt
 *   4. Save user to database
 *   5. Generate and return JWT
 *
 * Login flow:
 *   1. Delegate to AuthenticationManager (loads user, verifies BCrypt hash)
 *   2. Extract authenticated User from Authentication object
 *   3. Generate and return JWT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtUtil             jwtUtil;
    private final AuthenticationManager authenticationManager;

    // ── Registration ───────────────────────────────────────────

    /**
     * Registers a new user and returns a JWT so they are
     * immediately logged in after registering — no separate
     * login step required.
     *
     * @param request username, email, password
     * @return AuthResponse containing the JWT and user info
     * @throws AuthException if username or email is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Validate uniqueness before attempting to insert
        if (userRepository.existsByUsername(request.getUsername())) {
            throw AuthException.usernameTaken(request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw AuthException.emailTaken(request.getEmail());
        }

        // Build and persist the new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);

        log.info("New user registered: id={}, username={}",
                savedUser.getId(), savedUser.getUsername());

        // Generate JWT and return immediately — user is now logged in
        String token = jwtUtil.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    // ── Login ──────────────────────────────────────────────────

    /**
     * Authenticates a user and returns a JWT.
     *
     * Delegates password verification to Spring Security's
     * AuthenticationManager which internally:
     *   1. Calls UserDetailsServiceImpl.loadUserByUsername()
     *   2. Compares provided password against BCrypt hash
     *   3. Throws BadCredentialsException if mismatch
     *
     * @param request username and password
     * @return AuthResponse containing the JWT and user info
     * @throws AuthException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        try {
            // This triggers the full Spring Security auth flow
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Authentication succeeded — extract the User principal
            User user = (User) authentication.getPrincipal();

            String token = jwtUtil.generateToken(user);

            log.info("User logged in: id={}, username={}",
                    user.getId(), user.getUsername());

            return new AuthResponse(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username: {}",
                    request.getUsername());
            throw AuthException.invalidCredentials();
        }
    }
}