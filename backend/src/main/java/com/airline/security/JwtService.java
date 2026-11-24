package com.airline.security;

import com.airline.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final int accessMinutes;

    public JwtService(AppProperties properties) {
        byte[] secret = Base64.getDecoder().decode(properties.getJwt().getSecretBase64());
        this.key = Keys.hmacShaKeyFor(secret);
        this.issuer = properties.getJwt().getIssuer();
        this.accessMinutes = properties.getJwt().getAccessMinutes();
    }

    public String accessToken(CurrentUser user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessMinutes * 60L);
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public int getAccessMinutes() {
        return accessMinutes;
    }

    public static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 недоступен", ex);
        }
    }
}
