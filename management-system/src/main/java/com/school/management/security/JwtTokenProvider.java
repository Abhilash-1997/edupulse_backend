package com.school.management.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Generate JWT token for user
     * Payload contains only user ID
     */
    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return JWT.create()
                .withClaim("id", userId.toString())
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC256(jwtSecret));
    }

    /**
     * Extract user ID from JWT token
     */
    public UUID getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        String userId = decodedJWT.getClaim("id").asString();
        return UUID.fromString(userId);
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(jwtSecret))
                    .build()
                    .verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * Generate stream token for video streaming (expires in 2 hours)
     */
    public String generateStreamToken(UUID materialId, UUID userId, UUID schoolId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (2 * 60 * 60 * 1000)); // 2 hours

        return JWT.create()
                .withClaim("materialId", materialId.toString())
                .withClaim("userId", userId.toString())
                .withClaim("schoolId", schoolId.toString())
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC256(jwtSecret));
    }

    /**
     * Verify and decode stream token
     */
    public DecodedJWT verifyStreamToken(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC256(jwtSecret))
                .build()
                .verify(token);
    }
}