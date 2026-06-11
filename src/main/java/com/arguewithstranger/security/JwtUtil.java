package com.arguewithstranger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility for jjwt 0.12.x API.
 *
 * Key API changes from 0.11.x to 0.12.x:
 *   - Jwts.parserBuilder()     → Jwts.parser()
 *   - .setSigningKey()         → .verifyWith()
 *   - .parseClaimsJws()        → .parseSignedClaims()
 *   - Jwts.builder().setClaims() → .claims()
 *   - signWith(key, algorithm) → signWith(key) — algorithm inferred
 *   - Keys.hmacShaKeyFor(bytes) → same, still works
 *   - Decoders.BASE64          → removed, use standard Java
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Token Generation ───────────────────────────────────────

    /**
     * Generates a signed JWT for the authenticated user.
     *
     * Claims embedded:
     *   sub    → username
     *   role   → USER or ADMIN
     *   userId → database ID
     *   iat    → issued at
     *   exp    → expiry
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        if (userDetails instanceof com.arguewithstranger.entity.User user) {
            extraClaims.put("role",   user.getRole().name());
            extraClaims.put("userId", user.getId());
        }

        return buildToken(extraClaims, userDetails);
    }

    /**
     * Builds and signs the JWT using jjwt 0.12.x fluent API.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails) {

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Claim Extraction ───────────────────────────────────────

    /**
     * Extracts the username (subject) from a token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts the role claim from a token.
     */
    public String extractRole(String token) {
        return extractClaim(token,
                claims -> claims.get("role", String.class));
    }

    /**
     * Generic claim extractor using a function reference.
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and returns all claims from a token.
     *
     * jjwt 0.12.x API:
     *   Jwts.parser()                 (was parserBuilder())
     *       .verifyWith(key)          (was setSigningKey())
     *       .build()
     *       .parseSignedClaims(token) (was parseClaimsJws())
     *       .getPayload()             (was getBody())
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Token Validation ───────────────────────────────────────

    /**
     * Validates a token against the loaded UserDetails.
     *
     * Checks:
     *   1. Token username matches UserDetails username
     *   2. Token is not expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether a token has passed its expiration date.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates token structure and signature without a UserDetails object.
     * Used for quick pre-checks in the filter.
     */
    public boolean validateTokenStructure(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // ── Key Management ─────────────────────────────────────────

    /**
     * Derives the HMAC-SHA256 signing key from the secret string.
     *
     * jjwt 0.12.x returns SecretKey directly from
     * Keys.hmacShaKeyFor() — we pass the secret bytes directly
     * using UTF-8 encoding (no Base64 wrapping needed).
     *
     * The secret in application.properties must be at least
     * 32 characters for HS256 (256 bits minimum).
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}