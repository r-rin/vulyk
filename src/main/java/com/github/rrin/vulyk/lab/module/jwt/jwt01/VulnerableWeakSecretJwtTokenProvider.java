package com.github.rrin.vulyk.lab.module.jwt.jwt01;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.security.JwtProperties;
import com.github.rrin.vulyk.security.JwtTokenProvider;
import java.util.Date;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnLabEnabled(JwtWeakSecretLab.LAB_ID)
public class VulnerableWeakSecretJwtTokenProvider extends JwtTokenProvider {

    static final String WEAK_SECRET = "qwerty";

    private final JwtProperties properties;
    private final Algorithm weakAlgorithm;

    public VulnerableWeakSecretJwtTokenProvider(JwtProperties properties) {
        super(properties);
        this.properties = properties;
        this.weakAlgorithm = Algorithm.HMAC256(WEAK_SECRET);
    }

    @Override
    public String generateToken(String email, String authority) {
        return JWT.create()
            .withSubject(email)
            .withClaim("role", authority)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + properties.getExpiration()))
            .sign(weakAlgorithm);
    }

    @Override
    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return JWT.require(weakAlgorithm)
            .build()
            .verify(token);
    }
}
