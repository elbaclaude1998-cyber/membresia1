package com.membership.live.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio JWT mínimo para el control de acceso del WebSocket de directos.
 * Valida tokens (firma + expiración) y puede emitir tokens de desarrollo
 * para poder probar la señalización sin levantar el módulo de auth completo.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMs;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-ms:3600000}") long ttlMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMs = ttlMs;
    }

    public String generate(String subject) {
        return generate(subject, java.util.List.of());
    }

    public String generate(String subject, java.util.List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
