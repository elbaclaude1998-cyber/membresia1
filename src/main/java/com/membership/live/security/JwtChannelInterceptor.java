package com.membership.live.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Control de acceso JWT del WebSocket: en el frame STOMP CONNECT exige una
 * cabecera "Authorization: Bearer &lt;token&gt;". Si falta o es inválida, la
 * conexión se rechaza. El subject del token queda como Principal de la sesión.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader(AUTH_HEADER);
            if (header == null || !header.startsWith(BEARER)) {
                throw new IllegalArgumentException("Missing JWT on WebSocket CONNECT");
            }
            try {
                Claims claims = jwtService.parse(header.substring(BEARER.length()));
                accessor.setUser(new StompPrincipal(claims.getSubject()));
            } catch (JwtException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid JWT on WebSocket CONNECT", e);
            }
        }
        return message;
    }
}
