package com.paymentwallet.wallet_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(
            @Value("${jwt.secret}") String secret
    ) {
        byte[] keyBytes =
                Base64.getDecoder().decode(secret);

        this.signingKey =
                Keys.hmacShaKeyFor(keyBytes);
    }

    public Long extractUserId(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }
}