package com.github.rrin.vulyk.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final Algorithm algorithm;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC512(properties.getSecret());
    }

    public String generateToken(String email, String authority) {
        return JWT.create()
            .withSubject(email)
            .withClaim("role", authority)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + properties.getExpiration()))
            .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return JWT.require(algorithm)
            .build()
            .verify(token);
    }
}

