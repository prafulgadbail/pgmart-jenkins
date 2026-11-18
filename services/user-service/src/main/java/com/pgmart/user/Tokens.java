package com.pgmart.user;

import java.time.Duration;
import java.time.Instant;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Signs the JWT after login. catalog-service and order-service check it with the same secret. */
@Component
public class Tokens {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Duration ttl;

    public Tokens(@Value("${app.jwt-secret}") String secret, @Value("${app.jwt-ttl}") Duration ttl) {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer("pgmart").build();
        this.ttl = ttl;
    }

    public String issue(Users.User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer("pgmart")
                .withSubject(String.valueOf(user.id()))
                .withClaim("email", user.email())
                .withClaim("name", user.name())
                .withClaim("role", user.role())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(ttl))
                .sign(algorithm);
    }

    public AuthUser read(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ApiException.unauthorized();
        }
        try {
            DecodedJWT jwt = verifier.verify(authorization.substring(7));
            return new AuthUser(Long.parseLong(jwt.getSubject()), jwt.getClaim("email").asString(),
                    jwt.getClaim("name").asString(), jwt.getClaim("role").asString());
        } catch (JWTVerificationException | NumberFormatException e) {
            throw ApiException.unauthorized();
        }
    }
}
