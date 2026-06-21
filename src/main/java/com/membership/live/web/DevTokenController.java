package com.membership.live.web;

import com.membership.live.security.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SOLO DESARROLLO: emite un JWT válido para poder conectar al WebSocket de
 * directos sin el módulo de auth completo. Desactívalo en producción con
 * `security.jwt.dev-token-enabled=false`.
 */
@RestController
@ConditionalOnProperty(name = "security.jwt.dev-token-enabled", havingValue = "true", matchIfMissing = true)
public class DevTokenController {

    private final JwtService jwtService;

    public DevTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/live/dev-token")
    public Map<String, String> devToken(@RequestParam String userId) {
        return Map.of("token", jwtService.generate(userId));
    }
}
