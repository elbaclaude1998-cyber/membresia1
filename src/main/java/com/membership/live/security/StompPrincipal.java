package com.membership.live.security;

import java.security.Principal;

/** Principal autenticado en la sesión STOMP, derivado del subject del JWT. */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
