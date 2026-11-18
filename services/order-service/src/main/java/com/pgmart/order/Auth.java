package com.pgmart.order;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks the JWT that user-service signed. Controllers call {@link #customer} for shopper
 * endpoints and {@link #admin} for admin endpoints, with the Authorization header.
 */
@Component
public class Auth {

    private final JWTVerifier verifier;
    private final String internalToken;

    public Auth(@Value("${app.jwt-secret}") String secret, @Value("${app.internal-token}") String internalToken) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret)).withIssuer("pgmart").build();
        this.internalToken = internalToken;
    }

    public AuthUser user(String authorization) {
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

    /** Shopping (orders, wishlist) is for customers only; the admin account only runs the store. */
    public AuthUser customer(String authorization) {
        AuthUser user = user(authorization);
        if (user.isAdmin()) {
            throw ApiException.forbidden();
        }
        return user;
    }

    public AuthUser admin(String authorization) {
        AuthUser user = user(authorization);
        if (!user.isAdmin()) {
            throw ApiException.forbidden();
        }
        return user;
    }

    /** Calls between services (never routed from the internet) carry a shared token. */
    public void internal(String token) {
        if (token == null || !java.security.MessageDigest.isEqual(token.getBytes(), internalToken.getBytes())) {
            throw ApiException.forbidden();
        }
    }
}
