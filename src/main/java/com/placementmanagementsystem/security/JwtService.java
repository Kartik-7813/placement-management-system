package com.placementmanagementsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for creating, signing, parsing, and validating JSON Web Tokens (JWT).
 * Uses JJWT 0.12.6 APIs with HMAC-SHA256 signing.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long jwtExpirationMs;

    public JwtService(
            @Value("${application.security.jwt.secret}") String base64Secret,
            @Value("${application.security.jwt.expiration-ms:86400000}") long jwtExpirationMs
    ) {
        if (base64Secret == null || base64Secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret cannot be empty. Please configure application.security.jwt.secret.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(base64Secret.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("JWT secret must be a valid Base64-encoded string.", ex);
        }

        // HMAC-SHA256 requires at least 256 bits (32 bytes) of key material
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret key material must be at least 256 bits (32 bytes). Found: " + (keyBytes.length * 8) + " bits."
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Generates a signed JWT token for the specified user details with no extra claims.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a signed JWT token containing custom claims and the user subject (email).
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the subject (username / email) stored in the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration timestamp stored in the token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the token using a claims resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates whether a token is valid for the provided UserDetails.
     * Checks that the token's subject matches the username, the token is not expired,
     * and the signature is cryptographically valid.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username != null
                    && username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Checks if the token has expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * Parses and returns all claims from the JWT.
     * Uses JJWT 0.12.x Jwts.parser().verifyWith(key).build().parseSignedClaims(token).
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
