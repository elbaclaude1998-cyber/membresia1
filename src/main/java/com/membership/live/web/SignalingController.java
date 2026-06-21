package com.membership.live.web;

import com.membership.live.dto.SignalMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Relay de señalización WebRTC sobre STOMP.
 * Los clientes publican en  /app/live/{eventId}/signal
 * y reciben en             /topic/live/{eventId}/signal
 * filtrando por el campo `to` (clientId destino; vacío = broadcast/JOIN).
 *
 * La autenticación ya se garantizó en el CONNECT (JwtChannelInterceptor);
 * aquí solo se sella el usuario emisor para auditoría.
 */
@Controller
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    public SignalingController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/live/{eventId}/signal")
    public void signal(@DestinationVariable UUID eventId,
                       @Payload SignalMessage message,
                       Principal principal) {
        if (principal != null) {
            message.setSenderUser(principal.getName());
        }
        messagingTemplate.convertAndSend("/topic/live/" + eventId + "/signal", message);
    }
}
