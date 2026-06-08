package com.quill.security;

import com.quill.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        SecretKey key = jwtProperties.signingKey();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer("quill")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.expiration())))
                .signWith(key)
                .compact();
    }

    public String validateToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtProperties.signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
