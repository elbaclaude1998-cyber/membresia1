package com.membership.live.dto;

/**
 * Mensaje de señalización WebRTC relayado por STOMP entre host y viewers.
 * - type: JOIN | LEAVE | OFFER | ANSWER | ICE
 * - from / to: clientId de origen y destino (vacío = broadcast)
 * - sdp:       descripción de sesión (para OFFER / ANSWER)
 * - candidate: ICE candidate (para ICE)
 * - senderUser: lo rellena el servidor con el subject del JWT autenticado (auditoría)
 */
public class SignalMessage {

    private String type;
    private String from;
    private String to;
    private Object sdp;
    private Object candidate;
    private String senderUser;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public Object getSdp() { return sdp; }
    public void setSdp(Object sdp) { this.sdp = sdp; }

    public Object getCandidate() { return candidate; }
    public void setCandidate(Object candidate) { this.candidate = candidate; }

    public String getSenderUser() { return senderUser; }
    public void setSenderUser(String senderUser) { this.senderUser = senderUser; }
}
