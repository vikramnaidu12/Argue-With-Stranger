package com.arguewithstranger.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs once per HTTP request.
 *
 * Extends OncePerRequestFilter to guarantee single execution
 * per request even in complex filter chain scenarios (e.g.
 * forwarded requests inside the same servlet context).
 *
 * Filter flow:
 *  1. Extract "Authorization: Bearer <token>" header
 *  2. If header is absent or malformed → skip (chain continues)
 *  3. Extract username from token
 *  4. If SecurityContext already has authentication → skip
 *  5. Load UserDetails from database
 *  6. Validate token against UserDetails
 *  7. Set authentication in SecurityContext
 *  8. Continue filter chain
 *
 * Why we skip when SecurityContext already has auth:
 *   Prevents re-processing on internal forwards and avoids
 *   redundant DB calls for the same request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil            jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        // ── Step 1: Extract token from header ──────────────────
        final String token = extractTokenFromRequest(request);

        // ── Step 2: No token present → pass through ────────────
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 3: Extract username from token ────────────────
        final String username;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            // Token is malformed — log and pass through.
            // Spring Security will reject the request if the
            // endpoint requires authentication.
            log.debug("Failed to extract username from JWT: {}",
                    e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 4: Skip if already authenticated ──────────────
        if (username != null
                && SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            // ── Step 5: Load user from database ─────────────────
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            // ── Step 6: Validate token ───────────────────────────
            if (jwtUtil.isTokenValid(token, userDetails)) {

                // ── Step 7: Build authentication token ──────────
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                           // credentials null after auth
                                userDetails.getAuthorities()    // ROLE_USER or ROLE_ADMIN
                        );

                // Attach request details (IP, session) for audit logging
                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                // ── Step 8: Set in SecurityContext ───────────────
                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);

                log.debug("Authenticated user '{}' for request to {}",
                        username, request.getRequestURI());
            } else {
                log.debug(
                        "JWT validation failed for user '{}' on request to {}",
                        username, request.getRequestURI());
            }
        }

        // ── Step 9: Continue filter chain ──────────────────────
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the Authorization header.
     *
     * Expected header format: "Authorization: Bearer eyJhbGci..."
     *
     * Returns null (not throwing) if:
     *   - The Authorization header is absent
     *   - The header does not start with "Bearer "
     *   - The token portion is blank after trimming
     *
     * Returning null signals the filter to pass through without
     * authentication rather than reject the request — public
     * endpoints (/auth/**) must still be accessible.
     *
     * @param request the incoming HTTP request
     * @return the raw JWT string, or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken)
                && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }

    /**
     * Excludes authentication endpoints from JWT processing.
     * Requests to /auth/** never carry a JWT — they are the
     * endpoints that ISSUE the token. Processing them through
     * this filter is pointless and would log unnecessary warnings.
     *
     * Spring calls this before doFilterInternal — returning true
     * skips this filter entirely for matching paths.
     *
     * @param request the incoming HTTP request
     * @return true if this filter should be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/");
    }
}